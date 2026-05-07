package mc.md5g22.redbug.util;

public class VcdIdGenerator {

    StringBuilder id;

    char first_char = '!';
    char last_char = '~';


    boolean accessed = false;

    public VcdIdGenerator() {
        id = new StringBuilder("!");
    }

    public String next() {
        if (!accessed) {
            accessed = true;
            return id.toString();
        }

        char c = id.charAt(id.length() - 1);

        if (c == last_char) {
            id.append(first_char);
        } else {
            id.setCharAt(id.length()-1, (char) (c + 1));
        }

        return  id.toString();
    }

}
