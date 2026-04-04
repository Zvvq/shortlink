package com.cqie.shortlink_admin.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RedirectService {

    void redirect(String shortUrl, HttpServletRequest request, HttpServletResponse response);
}
