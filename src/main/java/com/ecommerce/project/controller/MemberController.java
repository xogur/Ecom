package com.ecommerce.project.controller;

import com.ecommerce.project.model.Member;
import com.ecommerce.project.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/post")
    public Member addMember(@RequestBody Member member){
        log.info("Member Request : " + member);

        Member result = memberService.addMember(member);

        log.info("Result Member : " + result);

        return result;
    }

    @GetMapping("/getMember/{memberId}")
    public Member getMember(@PathVariable String memberId){
        Member member = memberService.findById(memberId);

        return member;
    }
}
