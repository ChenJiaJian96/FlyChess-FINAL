//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.Serializable;

enum test implements Serializable {
    A(10),
    B(1);

    private int[] index = new int[10];

    private test(int index) {
        for(int i = 0; i < 10; ++i) {
            this.index[i] = index;
        }

    }

    public int getIndex() {
        return this.index[0];
    }

    public void setIndex(int index) {
        for(int i = 0; i < 10; ++i) {
            this.index[i] = index;
        }

    }

    public String toString() {
        return "test{index=" + this.index[0] + '}';
    }
}
