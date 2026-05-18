/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: "#2563eb",
                secondary: "#475569",
                danger: "#ef4444",
                success: "#22c55e",
            },
        },
    },
    plugins: [],
}

