package stark.dataworks.coderaider.genericagent.core.tool.builtin;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import stark.dataworks.coderaider.genericagent.core.tool.ToolCategory;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * PdfTextExtractTool extracts text content from local PDF files.
 */
public class PdfTextExtractTool extends AbstractBuiltinTool
{
    public PdfTextExtractTool(ToolDefinition definition)
    {
        super(definition, ToolCategory.FILE_SEARCH);
    }

    @Override
    public String execute(Map<String, Object> input)
    {
        String filePath = String.valueOf(input.getOrDefault("path", "")).trim();
        if (filePath.isEmpty())
        {
            return "PdfTextExtract(error): path is required.";
        }

        int maxChars = parseInt(input.get("max_chars"), 4000);
        try
        {
            Path path = Path.of(filePath);
            if (!Files.exists(path))
            {
                return "PdfTextExtract(error): file does not exist: " + filePath;
            }
            try (PDDocument document = Loader.loadPDF(path.toFile()))
            {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document).replace("\r\n", "\n").trim();
                if (text.length() <= maxChars)
                {
                    return text;
                }
                return text.substring(0, maxChars) + "\n...[truncated]";
            }
        }
        catch (Exception ex)
        {
            return "PdfTextExtract(error): " + ex.getMessage();
        }
    }

    private static int parseInt(Object raw, int defaultValue)
    {
        if (raw == null)
        {
            return defaultValue;
        }
        if (raw instanceof Number number)
        {
            return Math.max(100, number.intValue());
        }
        try
        {
            return Math.max(100, Integer.parseInt(raw.toString().trim()));
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }
}
