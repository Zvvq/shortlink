package com.cqie.shortlink_admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ShortLinkNotFoundController {

    @RequestMapping("/page/notfound")
    public String notFound() {
        return "not-found";
    }
}
