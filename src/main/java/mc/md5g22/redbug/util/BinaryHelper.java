package mc.md5g22.redbug.util;

public class BinaryHelper {

    /**
     *
     * @param value int to convert
     * @param bits number of bits for output
     * @return input value as unsigned binary string
     */
    public static String toUnsignedBinary(int value, int bits) {

        int max = (1 << bits) - 1;
        if (value < 0 || value > max) {
            return null;
        }

        StringBuilder str = new StringBuilder();

        for (int i = bits - 1; i >= 0; i--) {
            int bit = (value >>> i) & 1;
            str.append(bit);
        }

        return str.toString();
    }

    /**
     *
     * @param value int to convert
     * @param bits number of bits for output
     * @return input value a signed binary string
     */
    public static String toSignedBinary(int value, int bits) {

        int min = -(1 << (bits - 1));
        int max =  (1 << (bits - 1)) - 1;

        // Range check
        if (value < min || value > max) {
            return null;
        }

        StringBuilder str = new StringBuilder();

        // Mask to the bit width (simulate hardware truncation)
        int masked = value & ((1 << bits) - 1);

        for (int i = bits - 1; i >= 0; i--) {
            int bit = (masked >>> i) & 1;
            str.append(bit);
        }

        return str.toString();
    }

}
