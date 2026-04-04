package com.cqie.shortlink_admin.controller;


import com.cqie.shortlink_admin.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;


    @GetMapping("/{shortUrl}")
    public void redirect(@PathVariable String shortUrl, HttpServletRequest request, HttpServletResponse response) {
        redirectService.redirect(shortUrl, request, response);
    }
}
