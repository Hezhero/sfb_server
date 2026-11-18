package com.junchen.modules.utils;

import com.junchen.enums.PlatformEnum;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class TkOrderFee {
    double commission;
    String orderId;
    Date createTime;
    PlatformEnum platform;

    double rate;
}
