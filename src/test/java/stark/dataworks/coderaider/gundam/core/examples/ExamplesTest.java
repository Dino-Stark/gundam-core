package stark.dataworks.coderaider.gundam.core.examples;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class ExamplesTest
{
    public static final String ENV_FILE_PATH = ".env.local";

    public static HashMap<String, String> env;

    @BeforeAll
    public static void loadApiKeys() throws FileNotFoundException
    {
        // Simple implementation of .env parsing, this is not a production-ready solution.

        File envFile = new File(ENV_FILE_PATH);
        if (envFile.exists())
        {
            env = new HashMap<>();

            try (Scanner scanner = new Scanner(envFile))
            {
                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine().trim();
                    if (line.startsWith("#"))
                        continue;

                    int indexOfCommentBegin = line.indexOf('#');

                    if (indexOfCommentBegin > 0)
                        line = line.substring(0, indexOfCommentBegin);

                    int indexOfEquals = line.indexOf('=');
                    if (indexOfEquals > 0)
                    {
                        String key = line.substring(0, indexOfEquals).trim();
                        String value = line.substring(indexOfEquals + 1).trim();
                        env.put(key, value);
                    }
                }
            }
        }
    }

    @Test
    public void runExample01()
    {
        Example01SingleSimpleAgent.main(new String[0]);
//        System.out.println(new File("./").getAbsolutePath());
    }
}
