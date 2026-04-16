package com.controltower.app.finance.api.dto;

import java.time.Instant;

public record MarkPaidRequest(Instant paidAt) {}
