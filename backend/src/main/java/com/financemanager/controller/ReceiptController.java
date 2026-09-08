package com.financemanager.controller;

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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReceiptController {

    private final ChatModel chatModel;

    @Value("${tesseract.datapath:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tesseractDataPath;

    @PostMapping("/scan")
    public ResponseEntity<String> scanReceipt(@RequestParam("file") MultipartFile file) {
        // 1. Validate input file
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
            // 2. Preprocess & save file temporarily (downscale large images to avoid OOM)
            tempFile = preprocessAndSaveImage(file, fileName);

            // 3. Resolve tessdata datapath dynamically
            String datapath = resolveTessDataPath();
            System.out.println("Using Tesseract datapath: " + datapath);

            // 4. Run Tesseract OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(datapath);
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

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Throwable t) {
            System.err.println("OCR scan failed: " + t.getMessage());
            t.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process receipt image: " + t.getMessage()
            );
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File preprocessAndSaveImage(MultipartFile file, String fileName) throws Exception {
        File tempFile = File.createTempFile("receipt-", "-" + fileName);
        try (InputStream is = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                file.transferTo(tempFile);
                return tempFile;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int maxDimension = 1500;

            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                double scale = Math.min((double) maxDimension / originalWidth, (double) maxDimension / originalHeight);
                int targetWidth = (int) Math.round(originalWidth * scale);
                int targetHeight = (int) Math.round(originalHeight * scale);

                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                } finally {
                    g2d.dispose();
                }

                ImageIO.write(resizedImage, "jpg", tempFile);
                System.out.println("Resized receipt image from " + originalWidth + "x" + originalHeight + " to " + targetWidth + "x" + targetHeight);
            } else {
                file.transferTo(tempFile);
            }
        }
        return tempFile;
    }

    private String resolveTessDataPath() {
        // 1. Check system property or env var first
        String configured = System.getProperty("tesseract.datapath");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("TESSDATA_PREFIX");
        }
        if (configured == null || configured.trim().isEmpty()) {
            configured = tesseractDataPath;
        }

        if (configured != null && isValidTessDataDir(new File(configured))) {
            return new File(configured).getAbsolutePath();
        }

        // 2. Common Linux / Docker / Windows candidate locations
        String[] candidatePaths = {
            "/usr/share/tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/local/share/tessdata",
            "C:/Program Files/Tesseract-OCR/tessdata",
            "C:/Program Files (x86)/Tesseract-OCR/tessdata"
        };

        for (String path : candidatePaths) {
            File dir = new File(path);
            if (isValidTessDataDir(dir)) {
                System.out.println("Auto-detected valid Tesseract tessdata at: " + dir.getAbsolutePath());
                return dir.getAbsolutePath();
            }
        }

        return configured != null ? configured : "/usr/share/tessdata";
    }

    private boolean isValidTessDataDir(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File engData = new File(dir, "eng.traineddata");
            return engData.exists();
        }
        return false;
    }
}
