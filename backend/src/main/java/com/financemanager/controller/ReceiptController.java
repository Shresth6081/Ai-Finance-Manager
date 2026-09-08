package com.financemanager.controller;

import com.financemanager.config.OllamaHealthIndicator;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ChatModel chatModel;
    private final OllamaHealthIndicator ollamaHealthIndicator;

    @Value("${tesseract.datapath:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tesseractDataPath;

    @PostMapping("/scan")
    public ResponseEntity<String> scanReceipt(@RequestParam("file") MultipartFile file) {
        // 1. Verify if AI Model Server is active
        if (ollamaHealthIndicator != null && !ollamaHealthIndicator.isUp()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI model server is offline. Please make sure Ollama is running to scan receipts."
            );
        }

        // 2. Validate input file
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty.");
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (fileName == null || contentType == null ||
            (!contentType.startsWith("image/") &&
             !fileName.toLowerCase().endsWith(".png") &&
             !fileName.toLowerCase().endsWith(".jpg") &&
             !fileName.toLowerCase().endsWith(".jpeg"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PNG, JPG, and JPEG images are allowed."
            );
        }

        File tempFile = null;
        try {
            // 3. Save file temporarily
            tempFile = File.createTempFile("receipt-", "-" + fileName);
            file.transferTo(tempFile);

            // 4. Run Tesseract OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractDataPath);
            tesseract.setLanguage("eng");
            
            System.out.println("Running Tesseract OCR on file: " + fileName);
            String rawText = tesseract.doOCR(tempFile);
            System.out.println("OCR Completed. Extracted text length: " + (rawText != null ? rawText.length() : 0));

            if (rawText == null || rawText.trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Could not read any text from the receipt image. Please ensure it is clear and well-lit."
                );
            }

            // 5. Build prompt for Spring AI
            String prompt = "You are a receipts scanner assistant. Analyze the unstructured OCR text extracted from a purchase receipt:\n"
                    + "=== OCR EXTRACTED TEXT START ===\n"
                    + rawText + "\n"
                    + "=== OCR EXTRACTED TEXT END ===\n\n"
                    + "Your task is to parse the text and extract the details as a single JSON object. "
                    + "Choose one category from this list: [Food, Transport, Rent, Salary, Utilities, Shopping, Entertainment, Health].\n\n"
                    + "Strict Category Rules:\n"
                    + "- FOOD: If the receipt is from a grocery store, supermarket, fresh food market, restaurant, cafe, fast food, bakery, or is for general food items/ingredients.\n"
                    + "- SHOPPING: If the receipt is for clothes, electronics, general merchandise, department store items (e.g. clothing retail, furniture), or non-food general merchandise.\n"
                    + "- TRANSPORT: If the receipt is for gas stations, public transit, taxi, parking, tolls, or car service/parts.\n"
                    + "- UTILITIES: Water, electricity, garbage, phone, or internet bill statements.\n\n"
                    + "Identify:\n"
                    + "1. Merchant / Shop Name (save as key 'description')\n"
                    + "2. Total Purchase Amount (save as key 'amount' as decimal double number)\n"
                    + "3. Transaction Date in YYYY-MM-DD format (save as key 'date')\n"
                    + "4. Suggested category matching the merchant based on the rules (save as key 'category')\n\n"
                    + "Respond ONLY with a valid, clean JSON object. Do not include markdown wraps (like ```json), comments, or conversational text. "
                    + "JSON format:\n"
                    + "{\n"
                    + "  \"description\": \"merchant name\",\n"
                    + "  \"amount\": 0.00,\n"
                    + "  \"date\": \"YYYY-MM-DD\",\n"
                    + "  \"category\": \"categoryName\"\n"
                    + "}";

            System.out.println("Sending receipt text to Spring AI...");
            String aiResponse = chatModel.call(prompt);
            System.out.println("Spring AI Response: " + aiResponse);

            // Clean up backticks or markdown blocks in LLM output if present
            String cleanJsonResponse = aiResponse != null ? aiResponse.replaceAll("```json|```", "").trim() : "";

            return ResponseEntity.ok(cleanJsonResponse);

        } catch (Exception e) {
            System.err.println("OCR scan failed: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process receipt image: " + e.getMessage()
            );
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
