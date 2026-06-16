//import java.io.*;
//import java.nio.file.*;
//
//public class ReadAllCodeFilesRecursive {
//
//    public static void main(String[] args) {
//        // مسیر پوشه اصلی (این را به مسیر مورد نظر خود تغییر دهید)
//        String directoryPath = "C:\\your_folder_path";
//        String outputFilePath = "all_code_files_content.txt";
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
//            System.out.println("در حال پردازش فایل‌ها... لطفاً صبر کنید.");
//
//            Files.walk(Paths.get(directoryPath), Integer.MAX_VALUE)
//                .filter(Files::isRegularFile)
//                .filter(path -> {
//                    String name = path.toString().toLowerCase();
//                    String[] extensions = {
//                        // زبان‌های اصلی
//                        ".txt", ".java", ".py", ".js", ".ts", ".jsx", ".tsx",
//                        ".c", ".cpp", ".cc", ".cxx", ".h", ".hpp", ".cs",
//                        ".go", ".rb", ".php", ".swift", ".kt", ".kts", ".rs",
//                        ".scala", ".clj", ".groovy", ".dart",
//
//                        // وب
//                        ".html", ".htm", ".css", ".scss", ".sass", ".less",
//                        ".vue", ".svelte", ".xml", ".json",
//
//                        // اسکریپت و شل
//                        ".sh", ".bash", ".zsh", ".ps1", ".bat", ".cmd", ".pl", ".pm", ".lua",
//
//                        // دیتابیس و کانفیگ
//                        ".sql", ".yaml", ".yml", ".toml", ".ini", ".cfg", ".conf",
//                        ".properties", ".gradle", ".tf",
//
//                        // مستندات
//                        ".md", ".rst", ".adoc", ".tex",
//
//                        // سایر
//                        ".r", ".m", ".mm", ".erl", ".hs", ".elm"
//                    };
//
//                    for (String ext : extensions) {
//                        if (name.endsWith(ext)) {
//                            return true;
//                        }
//                    }
//                    return false;
//                })
//                .forEach(filePath -> {
//                    try {
//                        // نوشتن نام و مسیر کامل فایل
//                        writer.write("=".repeat(80));
//                        writer.newLine();
//                        writer.write("نام فایل: " + filePath.getFileName().toString());
//                        writer.newLine();
//                        writer.write("مسیر کامل: " + filePath.toString());
//                        writer.newLine();
//                        writer.write("=".repeat(80));
//                        writer.newLine();
//                        writer.newLine();
//
//                        // خواندن و نوشتن محتوای فایل
//                        try {
//                            String content = new String(Files.readAllBytes(filePath));
//                            writer.write(content);
//                        } catch (IOException e) {
//                            writer.write("⚠️ خطا در خواندن محتوای فایل: " + e.getMessage());
//                        }
//
//                        writer.newLine();
//                        writer.newLine();
//                        writer.newLine(); // سه خط خالی بین فایل‌ها برای جدا شدن بهتر
//
//                        // نمایش پیشرفت در کنسول
//                        System.out.println("✓ پردازش شد: " + filePath.toString());
//
//                    } catch (IOException e) {
//                        System.err.println("❌ خطا در نوشتن اطلاعات فایل " + filePath + ": " + e.getMessage());
//                    }
//                });
//
//            System.out.println("\n✅ عملیات با موفقیت به پایان رسید.");
//            System.out.println("📄 خروجی در فایل: " + outputFilePath);
//            System.out.println("📊 آمار: تمام فایل‌های کد با پسوندهای مشخص شده پردازش شدند.");
//
//        } catch (IOException e) {
//            System.err.println("❌ خطای اساسی: " + e.getMessage());
//            System.err.println("لطفاً مسیر پوشه را بررسی کنید و دوباره تلاش نمایید.");
//        }
//    }
//}