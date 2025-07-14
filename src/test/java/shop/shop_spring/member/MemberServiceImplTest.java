package shop.shop_spring.member;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import shop.shop_spring.email.dto.EmailDto;
import shop.shop_spring.email.EmailServiceImpl;
import shop.shop_spring.exception.DataNotFoundException;
import shop.shop_spring.member.dto.MemberCreationRequest;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.repository.MemberRepository;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.redis.RedisEmailAuthentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;


@SpringBootTest
@Transactional
public class MemberServiceImplTest {
    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailServiceImpl emailService;

    @Mock
    private RedisEmailAuthentication redisEmailAuthentication;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member createTestMember() {
        Member member = new Member();
        member.setId(1L);
        member.setUsername("test@example.com");
        member.setPassword("1234");
        member.setName("홍길동");
        member.setAddress("테스트시");
        member.setAddressDetail("테스트동");
        member.setBirthDate(LocalDate.of(1998, 1, 1));
        member.setNickname("테스트");

        return member;
    }

    @Test
    void formToMember_변환_성공() {
        MemberForm form = new MemberForm();
        form.setEmail("test@example.com");
        form.setPassword("pass");
        form.setName("홍길동");
        form.setAddress("서울");
        form.setAddressDetail("101동");
        form.setBirthDate("19990101");
        form.setNickname("gil");

        Member member = ReflectionTestUtils.invokeMethod(memberService, "formToMember", form);

        assertNotNull(member);
        assertEquals("홍길동", member.getName());
        assertEquals(LocalDate.of(1999, 1, 1), member.getBirthDate());
    }

    @Test
    void createRandomCode_6자리_숫자() {
        String code = ReflectionTestUtils.invokeMethod(memberService, "createRandomCode");

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void validateAuthenticationCode_일치하면_성공() {
        String email = "test@example.com";
        String code = "123456";
        when(redisEmailAuthentication.getEmailAuthenticationCode(email)).thenReturn("123456");

        assertDoesNotThrow(() -> memberService.validateAuthenticationCode(email, code));
    }

    @Test
    void validateAuthenticationCode_일치하지않으면_예외() {
        String email = "test@example.com";
        when(redisEmailAuthentication.getEmailAuthenticationCode(email)).thenReturn("999999");

        assertThrows(DataNotFoundException.class,
                () -> memberService.validateAuthenticationCode(email, "123456"));
    }

    @Test
    void validateUserInformation_정상정보_통과() {
        String username = "test@example.com";
        String name = "홍길동";
        String birthDate = "19990101";

        Member member = new Member();
        member.setUsername(username);
        member.setName(name);
        member.setBirthDate(LocalDate.of(1999, 1, 1));
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));

        assertDoesNotThrow(() -> memberService.validateUserInformation(username, name, birthDate));
    }

    @Test
    void validateUserInformation_이름불일치_예외() {
        String username = "test@example.com";
        Member member = new Member();
        member.setUsername(username);
        member.setName("김철수");
        member.setBirthDate(LocalDate.of(1999, 1, 1));
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));

        assertThrows(IllegalArgumentException.class,
                () -> memberService.validateUserInformation(username, "홍길동", "19990101"));
    }

    @Test
    void sendAuthenticationCode_성공() throws Exception {
        String email = "test@example.com";

        when(memberRepository.findByUsername(email)).thenReturn(Optional.empty());

        doNothing().when(redisEmailAuthentication)
                .setEmailAuthenticationExpire(eq(email), anyString(), anyLong());

        //
        doAnswer(invocation -> null)
                .when(emailService).sendMail(any(EmailDto.class));

        assertDoesNotThrow(() -> memberService.sendAuthenticationCode(email));
    }


    @Test
    void 회원가입_성공(){
        // given
        MemberCreationRequest request = new MemberCreationRequest();
        request.setUsername("test@example.com");
        request.setPassword("1234");
        request.setName("홍길동");
        request.setAddress("테스트시");
        request.setAddressDetail("테스트동");
        request.setBirthDate("19980101");
        request.setName("테스트");
        String encodedPassword = "encoded_password";

        when(passwordEncoder.encode("1234")).thenReturn(encodedPassword);
        when(memberRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());

        ArgumentCaptor<Member> memberArgumentCaptor = ArgumentCaptor.forClass(Member.class);

        // when
        memberService.createMember(request);

        // then
        verify(memberRepository, times(1)).save(memberArgumentCaptor.capture());
        verify(passwordEncoder, times(1)).encode("1234");

        Member savedMember = memberArgumentCaptor.getValue();

        assertNotNull(savedMember);
        assertEquals(request.getUsername(), savedMember.getUsername());
        assertEquals(encodedPassword, savedMember.getPassword());
    }

    @Test
    void 회원가입_실패_중복() {
        // given
        MemberCreationRequest request = new MemberCreationRequest();
        request.setUsername("test@example.com");
        request.setPassword("1234");
        request.setName("홍길동");
        request.setAddress("테스트시");
        request.setAddressDetail("테스트동");
        request.setBirthDate("19980101");
        request.setName("테스트");

        Member existingMember = createTestMember();

        when(memberRepository.findByUsername("test@example.com")).thenReturn(Optional.of(existingMember));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
           memberService.createMember(request);
        });

        verify(memberRepository, times(1)).findByUsername("test@example.com");
        verify(passwordEncoder, never()).encode("1234");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void 사용자ID기반_존재_회원_조회_성공(){
        // given
        Long existingMemberId = 1L;
        Member member = createTestMember();
        member.setId(existingMemberId);

        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));

        //when
        Member foundMember = memberService.findById(existingMemberId);

        //then
        assertNotNull(foundMember);
        assertEquals(existingMemberId, foundMember.getId());

        verify(memberRepository, times(1)).findById(existingMemberId);

    }

    @Test
    void 사용자ID기반_존재하지_않는_회원_조회_실패(){
        // given
        Long nonExistingMemberId = 1234L;

        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());
        //when & then
        assertThrows(DataNotFoundException.class, ()->
                memberService.findById(nonExistingMemberId)
        );

        verify(memberRepository, times(1)).findById(nonExistingMemberId);
    }

    @Test
    void 사용자이메일기반_존재_회원_조회_성공(){
        // given
        String existingUsername = "test@example.com";
        Member member = createTestMember();
        member.setUsername(existingUsername);

        when(memberRepository.findByUsername(anyString())).thenReturn(Optional.of(member));

        // when
        Member foundMember = memberService.findByUsername(existingUsername);

        // then
        assertNotNull(foundMember);
        assertEquals(existingUsername, foundMember.getUsername());

        verify(memberRepository, times(1)).findByUsername(existingUsername);
    }

    @Test
    void 사용자이메일기반_존재하지_않는_회원_조회_실패(){
        // given
        String nonExistingUsername = "test@example.com";

        when(memberRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThrows(DataNotFoundException.class, ()->
                memberService.findByUsername(nonExistingUsername)
        );

        verify(memberRepository, times(1)).findByUsername(nonExistingUsername);

    }

    @Test
    void 비밀번호_변경_성공(){
        //given
        String username = "test@example.com";
        String newRawPassword = "newPassword";
        String encodedNewPassword = "encodedNewPassword";

        Member member = createTestMember();
        member.setId(1L);
        member.setUsername(username);
        member.setPassword("oldPassword");

        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode(newRawPassword)).thenReturn(encodedNewPassword);
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        ArgumentCaptor<Member> memberArgumentCaptor = ArgumentCaptor.forClass(Member.class);

        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // when
        memberService.updatePassword(username, newRawPassword);

        // then
        verify(memberRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).encode(newRawPassword);
        verify(memberRepository, times(1)).save(memberArgumentCaptor.capture());

        Member capturedMember = memberArgumentCaptor.getValue();

        assertNotNull(capturedMember);
        assertEquals(encodedNewPassword, capturedMember.getPassword(), "Member 객체의 비밀번호가 업데이트 되어야함");
        assertEquals(encodedNewPassword, member.getPassword(), "비밀번호 업데이트 후 기존 Member 객체도 업데이트 되어야함");
        assertEquals(member.getId(), capturedMember.getId());
    }


    @Test
    void 회원_정보_모든_정보_수정(){
        // given
        Long existingId = 1L;
        String existingUsername = "test@example.com";
        String newRawPassword = "newPassword";
        String encodedNewRawPassword = "encodedPassword";
        LocalDate newBirthDate = LocalDate.of(1999,1, 1);
        String newAddress = "newAddress";
        String newAddressDetail = "newAddressDetail";
        String newNickname = "newNickname";

        Member existingMember = createTestMember();
        existingMember.setId(existingId);
        existingMember.setUsername(existingUsername);
        existingMember.setPassword("oldPassword");

        Member updatedMember = createTestMember();
        updatedMember.setId(existingId);
        updatedMember.setUsername(existingUsername);
        updatedMember.setPassword(newRawPassword);
        updatedMember.setBirthDate(newBirthDate);
        updatedMember.setAddress(newAddress);
        updatedMember.setAddressDetail(newAddressDetail);
        updatedMember.setNickname(newNickname);

        when(memberRepository.findByUsername(existingUsername)).thenReturn(Optional.of(existingMember));
        when(passwordEncoder.encode(newRawPassword)).thenReturn(encodedNewRawPassword);
        when(memberRepository.save(any(Member.class))).thenReturn(existingMember);

        ArgumentCaptor<Member> argumentCaptor = ArgumentCaptor.forClass(Member.class);

        // when
        memberService.updateMember(updatedMember);

        // then
        verify(memberRepository, times(1)).findByUsername(existingUsername);
        verify(passwordEncoder, times(1)).encode(newRawPassword);
        verify(memberRepository, times(1)).save(argumentCaptor.capture());

        Member capturedMember = argumentCaptor.getValue();
        assertNotNull(capturedMember, "save()에 전달된 인자가 null임");
        assertEquals(encodedNewRawPassword, capturedMember.getPassword(), "기존 회원의 비밀번호가 업데이트 되지 않음");
        assertEquals(newBirthDate, capturedMember.getBirthDate(), "기존 회원의 생일이 업데이트 되지 않음");
        assertEquals(newAddress, capturedMember.getAddress(), "기존 회원의 주소가 업데이트 되지 않음");
        assertEquals(newAddressDetail, capturedMember.getAddressDetail(), "기존 회원의 상세 주소가 업데이트 되지 않음");
        assertEquals(newNickname, capturedMember.getNickname(), "기존 회원의 상세 주소가 업데이트 되지 않음");

        assertEquals(existingMember.getId(), capturedMember.getId());
        assertEquals(existingMember.getUsername(), capturedMember.getUsername());
    }

    @Test
    void 회원_정보_일부_정보_수정(){
        // given
        Long existingId = 1L;
        String existingUsername = "test@example.com";
        String newAddress = "newAddress";
        String newAddressDetail = "newAddressDetail";

        Member existingMember = createTestMember();
        existingMember.setId(existingId);
        existingMember.setUsername(existingUsername);

        Member updatedMember = createTestMember();
        updatedMember.setPassword(null);
        updatedMember.setBirthDate(null);
        updatedMember.setAddress(newAddress);
        updatedMember.setAddressDetail(newAddressDetail);
        updatedMember.setNickname(null);

        when(memberRepository.findByUsername(existingUsername)).thenReturn(Optional.of(existingMember));

        ArgumentCaptor<Member> argumentCaptor = ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(any(Member.class))).thenReturn(existingMember);

        // when
        memberService.updateMember(updatedMember);

        // then
        verify(memberRepository, times(1)).findByUsername(existingUsername);
        verify(memberRepository, times(1)).save(argumentCaptor.capture());
        verify(passwordEncoder, never()).encode(anyString());

        Member capturedMember = argumentCaptor.getValue();
        assertNotNull(capturedMember);
        // 바뀐거 체크
        assertEquals(newAddress, capturedMember.getAddress(), "기존 회원의 주소가 업데이트되지 않음");
        assertEquals(newAddressDetail, capturedMember.getAddressDetail(), "기존 회원의 주소가 업데이트 되지않음");
        // 바뀌지 앟은거 체크
        assertEquals("1234", capturedMember.getPassword(), "비밀번호가 업데이트됨");
        assertEquals("테스트", capturedMember.getNickname(), "닉네임이 업데이트됨");
        assertEquals(LocalDate.of(1998, 1, 1), capturedMember.getBirthDate(), "생일이 업데이트됨");

        assertEquals(existingMember.getId(), capturedMember.getId());
        assertEquals(existingMember.getUsername(), capturedMember.getUsername());
    }

    @Test
    void 회원_정보_수정_없음(){
        // given
        String existingUsername = "test@example.com";

        Member existingMember = createTestMember();
        existingMember.setUsername(existingUsername);

        Member updatedMember = createTestMember();
        updatedMember.setUsername(existingUsername);
        updatedMember.setName(null);
        updatedMember.setNickname(null);
        updatedMember.setAddressDetail(null);
        updatedMember.setAddress(null);
        updatedMember.setPassword(null);

        when(memberRepository.findByUsername(existingUsername)).thenReturn(Optional.of(existingMember));

        ArgumentCaptor<Member> argumentCaptor = ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(any(Member.class))).thenReturn(existingMember);

        // when
        memberService.updateMember(updatedMember);

        // then
        verify(memberRepository, times(1)).findByUsername(existingUsername);
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, times(1)).save(argumentCaptor.capture());

        Member capturedMember = argumentCaptor.getValue();
        Member temp = createTestMember();
        temp.setUsername(existingUsername);
        assertNotNull(capturedMember);
        assertEquals(temp.getPassword(), capturedMember.getPassword(), "기존 회원 비밀버호가 업데이트됨");
        assertEquals(temp.getAddress(), capturedMember.getAddress(), "기존 회원 주소가 업데이트됨");
        assertEquals(temp.getAddressDetail(), capturedMember.getAddressDetail(), "기존 회원 상세주소가 업데이트됨");
        assertEquals(temp.getBirthDate(), capturedMember.getBirthDate(), "기존 회원 생일이 업데이트됨");
        assertEquals(temp.getNickname(), capturedMember.getNickname(), "기존 회원의 닉네임이 업데이트됨");

        assertEquals(existingMember.getId(), capturedMember.getId());
        assertEquals(existingMember.getUsername(), capturedMember.getUsername());

    }
}