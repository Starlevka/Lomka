package lomka.starl.duck;

public interface IGlyphSource {
    float getAdvance(int codepoint, boolean bold);
    default void lomka$clear() {}
}