package shop.shop_spring.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import shop.shop_spring.exception.DataNotFoundException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.repository.MemberRepository;
import shop.shop_spring.security.model.MyUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MyUserDetailService implements UserDetailsService {
    private final MemberRepository memberRepository;

    /** 로그인 페이지에서 로그인 시 사용자 정보를 DB에서 꺼내는 코드
     *  DB에서 가져온 유저정보와 사용자가 폼에서 제출한 아이디 비번 정보를 비교
     * @return 로그인 성공 및 로그인 유저에게 쿠키 전송
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // DB로부터 username을 가진 유저 정보의 비밀번호와 체출한 비밀번호를 비교
        // 비밀번호만 넣어주면 Spring Security가 알아허 새줌
        // username로 DB의 정보 로드(로그인한 아이디 or 이메일)
        Optional<Member> result = memberRepository.findByUsername(username);
        if (result.isEmpty()){
            throw new DataNotFoundException("유효하지 않은 이메일 주소");
        }
        Member member = result.get();
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(member.getRole().name()));

        MyUser myUser = new MyUser(member.getUsername(), member.getPassword(), authorities);
        myUser.setId(member.getId());
        myUser.setName(member.getName());
        if (member.getNickname() == null){
            myUser.setNickname("null");
        } else{
            myUser.setNickname(member.getNickname());
        }
        myUser.setRole(member.getRole());

        return myUser; // 컨트롤러의 Authentication auth 파라미터
    }
}
