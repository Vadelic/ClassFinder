import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isUpperCase;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Paths.get;

class ClassFinder {
    private final List<String> patternMasks;

    ClassFinder(String pattern) throws IllegalArgumentException {
        if (pattern == null || pattern.length() == 0 || " ".equals(pattern)) {
            throw new IllegalArgumentException("Pattern must include letters.");
        }
        if (!pattern.codePoints().filter(Character::isUpperCase).findFirst().isPresent()
                && !pattern.contains(" ")
                && !pattern.contains("*")) {
            pattern = pattern.toUpperCase();
        }
        this.patternMasks = splitToWords(pattern);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            return;
        }
        ClassFinder classFinder = new ClassFinder(args[1]);
        readAllLines(get(args[0]))
                .stream()
                .filter(classFinder::match)
                .map(ClassName::new)
                .sorted((Comparator.comparing(o -> o.className)))
                .forEach(System.out::println);
    }

    public boolean match(String fullClassName) {

        ClassName classNameObj = new ClassName(fullClassName);
        String className = classNameObj.className;
        if (className.contains(" ")) {
            System.out.println("Incorrect class name: " + classNameObj);
            return false;
        }
        int cursor = 0;
        for (int i = 0; i < patternMasks.size(); i++) {
            String mask = patternMasks.get(i);

            if (i == patternMasks.size() - 1 && mask.endsWith(" ")) {
                return isLastWord(mask.trim(), className, cursor);
            } else {
                cursor = moveCursorAfterMask(mask, className, cursor);
            }

            if (cursor == -1) {
                return false;
            }
        }
        return true;
    }

    private int moveCursorAfterMask(String mask, String string, int cursor) {
        if (!mask.contains("*")) {
            cursor = string.indexOf(mask, cursor);
            return cursor == -1
                    ? -1
                    : cursor + mask.length();
        }

        char[] maskCharArray = mask.toCharArray();
        for (int iMask = 0; iMask < maskCharArray.length; ) {
            char cMask = maskCharArray[iMask];
            if (cMask == '*') {
                cursor++;
                iMask++;
            } else {
                String sequence = extractCharSequence(maskCharArray, iMask);
                iMask += sequence.length();

                int idx = string.indexOf(sequence, cursor);
                if (idx == -1) {
                    return -1;
                } else {
                    cursor = idx + sequence.length();
                }
            }
        }
        return cursor > string.length() ? -1 : cursor;
    }

    private String extractCharSequence(char[] charArray, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < charArray.length; i++) {
            char c = charArray[i];
            if (isAlphabetic(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private boolean isLastWord(String mask, String camelCase, int startIndex) {
        if (camelCase.length() == 0 || mask.length() == 0) {
            return false;
        }
        List<String> words = splitToWords(camelCase);
        String lastWord = words.get(words.size() - 1);

        if (camelCase.indexOf(lastWord) >= startIndex) {
            if (mask.contains("*")) {
                return moveCursorAfterMask(mask, lastWord, 0) != -1;
            } else {
                return lastWord.contains(mask);
            }
        } else {
            return false;
        }
    }

    private List<String> splitToWords(String camelCase) {
        List<String> result = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (isUpperCase(c) && builder.length() != 0) {
                result.add(builder.toString());
                builder = new StringBuilder();
            }
            builder.append(c);
        }
        if (builder.length() != 0) {
            result.add(builder.toString());
        }
        return result;
    }

    static class ClassName {
        private final String packageName;
        private final String className;

        private ClassName(String fullClassName) {
            int index = fullClassName.lastIndexOf('.');
            if (index == -1) {
                packageName = "";
                className = fullClassName.trim();
            } else {
                packageName = fullClassName.substring(0, index).trim();
                className = fullClassName.substring(index + 1).trim();
            }
        }


        @Override
        public String toString() {
            return String.format("%s%s", packageName.length() == 0 ? "" : packageName + ".", className);
        }
    }
}
