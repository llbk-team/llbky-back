package com.example.demo.learning.dto.request;

import lombok.Data;

@Data
public class RoadmapRefineRequest {
  private String originalRoadmapJson;
  private String userFeedback;
}
