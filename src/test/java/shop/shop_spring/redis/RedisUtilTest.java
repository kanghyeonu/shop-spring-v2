package shop.shop_spring.redis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.context.ActiveProfiles;

@Disabled
@SpringBootTest
@ActiveProfiles("redis")
public class RedisUtilTest {
    @Autowired private RedisUtil redisUtil;
    @Autowired private StringRedisTemplate redisTemplate; // 테스트 후 정리용

    String key = "testKey";
    String value = "testValue";

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void 기본_만료_데이터_입력_조회() throws InterruptedException{
        // Given: 만료 시간 설정
        long duration = 1;

        // When: 데이터 저장 테스트
        redisUtil.setDataExpire(key, value, duration);

        // Then: 데이터 저장, 만료 확인
        assertEquals(value, redisUtil.getData(key));
        assertTrue(redisUtil.existData(key));

        Thread.sleep(duration * 1000 + 500);
        assertNull(redisUtil.getData(key));
        assertFalse(redisUtil.existData(key));
    }

    @Test
    void 기본_데이터_삭제() throws InterruptedException{
        // Given: 삭제할 데이터 저장
        redisTemplate.opsForValue().set(key, value);
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(key))
                , "데이터 삭제 전에는 데이터가 존재함");

        // When: 삭제 연산
        redisUtil.deleteData(key);

        // Then
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(key))
                , "데이터 삭제 후에는 데이터가 없어야함");
    }

    @Test
    void 데이터_존재_여부_확인(){
        // Given: 저장할 키(key), 저장하지 않을 키(keyNotExists)
        String keyNotExists = "NotExistTestKey";
        redisTemplate.opsForValue().set(key, value);

        // When: 조회
        boolean exists = redisUtil.existData(key);
        boolean notExist = redisUtil.existData(keyNotExists);

        // Then
        assertTrue(exists, "저장한 키는 존재");
        assertFalse(notExist, "저장하지 않은 키는 없음");
    }
    @AfterEach
    void tearDown() {
        // 테스트 종료 후 Redis 상태를 다시 초기화 (정리)
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
}
