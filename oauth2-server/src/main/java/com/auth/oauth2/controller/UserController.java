package com.auth.oauth2.controller;

import com.auth.oauth2.domain.dto.request.SignupRequest;
import com.auth.oauth2.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/login")
  public String showLoginForm(
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String logout,
      @RequestParam(required = false) String redirectUri,
      @RequestParam(required = false) String clientId,
      Model model) {
    if (error != null) {
      model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
    if (logout != null) {
      model.addAttribute("logout", true);
    }
    model.addAttribute("redirectUri", redirectUri);
    model.addAttribute("clientId", clientId);
    return "login";
  }

  @GetMapping("/signup")
  public String showSignupForm(
      @RequestParam(required = false) String redirectUri,
      @RequestParam(required = false) String clientId,
      Model model) {
    model.addAttribute("signupRequest", new SignupRequest());
    model.addAttribute("redirectUri", redirectUri);
    model.addAttribute("clientId", clientId);
    return "signup";
  }

  @PostMapping("/signup")
  public String signup(
      @Valid SignupRequest signupRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("redirectUri", signupRequest.getRedirectUri());
      model.addAttribute("clientId", signupRequest.getClientId());
      return "signup";
    }

    try {
      userService.signup(signupRequest);

      // 회원가입 성공 시 리다이렉트
      if (signupRequest.getRedirectUri() != null && !signupRequest.getRedirectUri().isEmpty()) {
        return "redirect:" + signupRequest.getRedirectUri() + "?success=true";
      } else {
        redirectAttributes.addFlashAttribute("signupSuccess", true);
        return "redirect:/login";
      }
    } catch (RuntimeException e) {
      model.addAttribute("error", e.getMessage());
      model.addAttribute("redirectUri", signupRequest.getRedirectUri());
      model.addAttribute("clientId", signupRequest.getClientId());
      return "signup";
    }
  }
}

