/*-
 * Copyright (c) 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Usage: java RemoveAnnotations.java -f <list of files> -a <list of annotations>
 * NOTE: The script does not recognize whether the annotaiton is inside a comment
 * or inside a string.
 * @author Marián Konček
 */
class RemoveAnnotations
{
    List<String> annotations = new ArrayList<>();
    List<Path> files = new ArrayList<>();

    void parseArguments(String[] args)
    {
        for (int i = 0; i != args.length; ++i)
        {
            if (args[i].equals("-a") || args[i].equals("--annotations"))
            {
                ++i;
                while (i != args.length && !args[i].startsWith("-"))
                {
                    annotations.add(args[i]);
                    ++i;
                }
                --i;
            }
            else if (args[i].equals("-f") || args[i].equals("--files"))
            {
                ++i;
                while (i != args.length && !args[i].startsWith("-"))
                {
                    files.add(Paths.get(args[i]));
                    ++i;
                }
                --i;
            }
            else
            {
                throw new IllegalArgumentException("Unrecognized option \"" + args[i] + "\"");
            }
        }
    }

    static int skipString(String string, int begin)
    {
        int result = begin + 1;
        assert(string.charAt(begin) == '"');
        int numBackslashes = 0;
        while (string.charAt(result) != '"' && numBackslashes % 2 == 0)
        {
            if (string.charAt(result) == '\\')
            {
                ++numBackslashes;
            }
            else
            {
                numBackslashes = 0;
            }
            ++result;
        }
        return result + 1;
    }

    static int skipComment(String string, int begin)
    {
        int result = begin + 3;
        assert(string.charAt(begin) == '/');
        assert(string.charAt(begin + 1) == '*');
        while (string.charAt(result) != '/' || string.charAt(result - 1) != '*')
        {
            ++result;
        }
        return result + 1;
    }

    static int readWhiteSpaceAndskipComments(String string, int begin)
    {
        if (begin + 1 == string.length() && Character.isWhitespace(string.charAt(begin)))
        {
            return begin + 1;
        }
        while (begin < string.length())
        {
            if (Character.isWhitespace(string.charAt(begin)))
            {
                ++begin;
                continue;
            }
            if (begin + 1 < string.length())
            {
                if (string.charAt(begin) == '/' &&
                        string.charAt(begin + 1) == '*')
                {
                    begin = skipComment(string, begin);
                    continue;
                }
                if (string.charAt(begin) == '/' &&
                        string.charAt(begin + 1) == '/')
                {
                    begin = string.indexOf(System.lineSeparator(), begin) + System.lineSeparator().length();
                    if (begin == -1)
                    {
                        begin = string.length();
                    }
                    continue;
                }
            }
            break;
        }
        return begin;
    }

    String removeImports(String content)
    {
        String result = "";
        skipLine: for (String line : content.split("\\R"))
        {
            if (line.startsWith("import "))
            {
                for (String annotation : annotations)
                {
                    if (line.endsWith("." + annotation + ";"))
                    {
                        continue skipLine;
                    }
                }
            }
            result += line + System.lineSeparator();
        }
        return result;
    }

    String removeUsage(String content)
    {
        for (String annotation : annotations)
        {
            int index = 0;
            String newContent = "";
            int foundUsage;
            while (true)
            {
                foundUsage = content.indexOf("@" + annotation, index);

                if (foundUsage == -1)
                {
                    foundUsage = content.length();
                }
                newContent += content.substring(index, foundUsage);
                index = foundUsage;

                if (foundUsage == content.length())
                {
                    break;
                }

                int annotationEnd = foundUsage + annotation.length() + 1;
                if (annotationEnd < content.length())
                {
                    if (content.charAt(annotationEnd) != '(' && !Character.isWhitespace(content.charAt(annotationEnd)))
                    {
                        newContent += content.substring(index, annotationEnd);
                        index = annotationEnd;
                        continue;
                    }
                    if (annotationEnd < content.length() && content.charAt(annotationEnd) == '(')
                    {
                        ++annotationEnd;
                        while (annotationEnd < content.length() && content.charAt(annotationEnd) != ')')
                        {
                            if (content.charAt(annotationEnd) == '"')
                            {
                                annotationEnd = skipString(content, annotationEnd);
                                continue;
                            }
                            if (content.charAt(annotationEnd) == '*' &&
                                    content.charAt(annotationEnd - 1) == '/')
                            {
                                annotationEnd = skipComment(content, annotationEnd);
                                continue;
                            }
                            if (content.charAt(annotationEnd) == '/' &&
                                    content.charAt(annotationEnd - 1) == '/')
                            {
                                annotationEnd = content.indexOf(System.lineSeparator(), annotationEnd) + System.lineSeparator().length();
                                continue;
                            }
                            ++annotationEnd;
                        }
                        ++annotationEnd;
                    }
                    index = annotationEnd;
                }
            }
            content = newContent;
        }
        return content;
    }

    void apply() throws IOException
    {
        for (var file : files)
        {
            String content;
            try (var fis = new FileInputStream(file.toFile()))
            {
                content = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                content = content.replaceAll("\\R", System.lineSeparator());
            }
            content = removeImports(content);
            content = removeUsage(content);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    public static void main(String[] args) throws IOException
    {
        var self = new RemoveAnnotations();
        self.parseArguments(args);
        self.apply();
    }
}
