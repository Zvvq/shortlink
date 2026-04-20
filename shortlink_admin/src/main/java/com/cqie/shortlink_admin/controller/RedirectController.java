package com.cqie.shortlink_admin.controller;


import com.cqie.shortlink_admin.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;


    @GetMapping("/{shortUrl}")
    public void redirect(@PathVariable String shortUrl, HttpServletRequest request, HttpServletResponse response) {
        redirectService.redirect(shortUrl, request, response);
    }

    @ResponseBody
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @ResponseBody
    @GetMapping("/test2")
    public String test2() {
        return "test2";
    }
}
