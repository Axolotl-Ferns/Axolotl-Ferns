package axl.ferns.player.generation;

import axl.ferns.server.Priority;
import axl.ferns.server.Server;
import axl.ferns.server.player.Player;
import axl.ferns.server.service.Service;
import axl.ferns.server.service.ServiceBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCodegenTest {

    @Test
    void playerCodegen() {
        assertDoesNotThrow(() -> {
            new Server(new ArrayList<>() {{
                add(new ServiceBaseTest());
            }}, 2292, 6, 20);

            {
                Player player = Server.getInstance().newPlayer();
                player.setId(1);
                Server.getInstance().getPlayers().add(player);
            }

            {
                Player player = Server.getInstance().getPlayerById(1);
                assertNotNull(player);
                assertInstanceOf(PlayerGenerationReferenceTest.class, player);
                ((PlayerGenerationReferenceTest) player).setX(23_10_2022).setX(33);
                assertEquals(33, ((PlayerGenerationReferenceTest) player).getX());
            }
        });
    }

    @Service(
            name = "ServiceBaseTest",
            priority = Priority.SERVER,
            version = "test"
    )
    public static class ServiceBaseTest extends ServiceBase {

        @Override
        public void onEnable() {
            Server.getInstance().registerPlayerAddition(PlayerGenerationReferenceTest.class);
        }

    }

}