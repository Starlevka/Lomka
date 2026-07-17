//? if >=1.21.11 {
package lomka.starl.utils;

public class TextureUtilState {
    public int[] colorBuffer = new int[0];
    public int[] distanceBuffer = new int[0];
    public int[] queue = new int[0];

    public void ensureCapacity(int totalPixels) {
        if (this.colorBuffer.length < totalPixels) {
            this.colorBuffer = new int[totalPixels];
            this.distanceBuffer = new int[totalPixels];
            this.queue = new int[totalPixels];
        }
    }
}
//?}
