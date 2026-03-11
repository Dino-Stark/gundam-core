package stark.dataworks.coderaider.genericagent.core.tool.builtin;

import org.junit.jupiter.api.Test;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;
import stark.dataworks.coderaider.genericagent.core.tool.ToolParameterSchema;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfTextExtractToolTest
{
    @Test
    public void shouldExtractTextFromPdf()
    {
        PdfTextExtractTool tool = new PdfTextExtractTool(new ToolDefinition(
            "pdf_text_extract",
            "Extract text from local PDF files",
            List.of(
                new ToolParameterSchema("path", "string", true, "PDF file path"),
                new ToolParameterSchema("max_chars", "number", false, "Maximum characters to return")
            )));

        String output = tool.execute(Map.of(
            "path", "src/test/resources/inputs/3D Convolutional Neural Networks for Human Action Recognition.pdf",
            "max_chars", 1500));

        assertFalse(output.isBlank());
        assertTrue(output.toLowerCase().contains("convolutional") || output.toLowerCase().contains("human"));
    }
}
