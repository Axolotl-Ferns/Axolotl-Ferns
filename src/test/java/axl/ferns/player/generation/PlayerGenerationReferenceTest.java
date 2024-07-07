package axl.ferns.player.generation;

import axl.ferns.server.player.*;
import axl.ferns.server.player.PlayerInterface;

@PlayerAdditions(
        fields = {
                @PlayerField(name = "x", type = int.class)
        }
)
public interface PlayerGenerationReferenceTest extends PlayerInterface {

        @PlayerGetter(name = "x")
        int getX();

        @PlayerSetter(name = "x")
        PlayerGenerationReferenceTest setX(int x);

}
