package com.plgdhd.authservice.dto.response;

import java.util.List;

public record UserInfoResponse(

        String userId,
        String email,
        String username,
        List<String> roles // TODO переделать через enum, сейчас три часа ночи и мне лень
) {}
