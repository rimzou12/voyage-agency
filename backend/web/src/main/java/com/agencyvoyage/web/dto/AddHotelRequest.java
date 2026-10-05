package com.agencyvoyage.web.dto;

import java.util.List;

public record AddHotelRequest(String name, String description, List<String> photoUrls, List<String> amenities) {}
