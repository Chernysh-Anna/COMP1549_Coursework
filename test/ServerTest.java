import model.Member;
import model.Message;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {

    // Test 1: Check Member to save data rightly
    @Test
    void testMemberCreation() {
        Member member = new Member("Anna", "127.0.0.1", 1234);

        assertEquals("Anna", member.getId());
        assertEquals("127.0.0.1", member.getIp());
        assertEquals(1234, member.getPort());
        assertFalse(member.isCoordinator());
    }

    // Test 2: Coordinator assignment
    @Test
    void testCoordinatorAssignment() {
        Member member = new Member("Kateryna", "127.0.0.1", 5001);
        assertFalse(member.isCoordinator());

        member.setCoordinator(true);
        assertTrue(member.isCoordinator());

        member.setCoordinator(false);
        assertFalse(member.isCoordinator());
    }

    // Test 3: Message serialization
    @Test
    void testMessageSerializeDeserialize() {
        Message original = new Message(
                Message.Type.BROADCAST, "Tetiana", null, "Hello everyone!"
        );

        String serialized = original.serialize();
        Message restored = Message.deserialize(serialized);

        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getFromId(), restored.getFromId());
        assertEquals(original.getText(), restored.getText());
        assertNull(restored.getToId());
    }

    // Test 4: Private message
    @Test
    void testPrivateMessage() {
        Message msg = new Message(
                Message.Type.PRIVATE, "Anna", "Kateryna", "Hello Kateryna!"
        );

        assertEquals(Message.Type.PRIVATE, msg.getType());
        assertEquals("Anna", msg.getFromId());
        assertEquals("Kateryna", msg.getToId());
        assertEquals("Hello Kateryna!", msg.getText());

        // Serialize and check format
        // Серіалізуємо і перевіряємо формат
        String serialized = msg.serialize();
        assertTrue(serialized.startsWith("PRIVATE|Anna|Kateryna|"));
    }
}

//test ideas
//test 5: Broadcast message
//test 6: Member ping update
// Test 7: New coordinator election when coordinator leaves
// Test 8: Member removal updates registry correctly