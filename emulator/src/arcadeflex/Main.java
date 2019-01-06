/**
 * ported to 0.37b7
 */
package arcadeflex;

import static old2.arcadeflex.libc_v2.*;
import old.arcadeflex.osdepend;

public class Main {

    public static void main(String[] args) {
        ConvertArguments("arcadeflex", args);
        System.exit(osdepend.main(argc, argv));
    }
}
