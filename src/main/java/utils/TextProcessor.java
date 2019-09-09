package utils;

public class TextProcessor {
    public static char toHalfAngle(char c) {
        if (c == '（')
            c = '(';
        else if (c == '）')
            c = ')';
        else if (c == '【')
            c = '[';
        else if (c == '】')
            c = ']';
        else
        {
            // 转半角
            if (c == 12288)
                c = ' ';
            else if (c > 65280 && c < 65375)    // 全角字母转半角
                c = (char)(c - 65248);
        }
        return c;
    }

    public static String normalizeComName(String name) {
        if (name == null || "".equals(name)) return null;

        int charsCountInBracket = 0;  // 位于括号中的字数
        int bracketDepth = 0;       // 括号嵌套深度
        StringBuilder sb = new StringBuilder(name.length());
        StringBuilder buf1 = new StringBuilder();
        StringBuilder buf = new StringBuilder();          // 括号以及其中的有效字符存入缓存

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            char ch = toHalfAngle(c);
            if ((ch >= '\u4e00' && ch <= '\u9fa5') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' || ch <= 'Z')) {     //! --- 增加了英文作为valid字符 -----
                if (bracketDepth > 0) {
                    // 当前字符位于括号中
                    charsCountInBracket++;
                    buf.append(ch);         // 括号中字符刷入buf
                }
                else {
                    // 当前字符不在括号中
                    if (buf.length() > 0) {
                        sb.append(buf.toString());
                        buf.delete(0, buf.length());
                    }
                    sb.append(ch);
                }
            }
            else {
                if (ch == '(' || ch == '[') {
                    bracketDepth++;
                    buf.append(ch);
                }
                else if (ch == ')' || ch == ']') {
                    if (bracketDepth > 0) {
                        bracketDepth--;
                        buf.append(ch);
                        if (bracketDepth == 0 && charsCountInBracket > 0) {
                            // 括号已经完全关闭，且括号中有中文字符，
                            sb.append(buf.toString());
                            buf.delete(0, buf.length());
                            charsCountInBracket = 0;
                        }
                    }
                    // 括号已经关闭，则多余的右括号则丢弃
                }
                // 英文字符缓存
                else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' || ch <= 'Z')) {
                    buf.append(ch);
                    charsCountInBracket++;
                }
                else {
                    // 丢弃其他字符
                }
            }
        }
        // 判断括号是否关闭
        if (bracketDepth > 0) {
            if (charsCountInBracket > 0) {
                sb.append(buf.toString());
            }
        }
        return sb.toString();
    }
}
