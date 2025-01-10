package com.paicbd.module.dto;

import com.paicbd.smsc.utils.Generated;
import lombok.Getter;
import lombok.Setter;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.service.sms.IpSmGwGuidance;
import org.restcomm.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.MWStatus;

@Getter
@Setter
@Generated
public class MapRoutingData {

    //SendRoutingInfoForSMResponse Data
    private IMSI imsi;
    private LocationInfoWithLMSI locationInfoWithLMSI;
    private IpSmGwGuidance ipSmGwGuidance;
    private Boolean mwdSet;

    //InformServiceCentreRequest Data
    private boolean informServiceCenterData;
    private ISDNAddressString storedMSISDN;
    private MWStatus mwStatus;
    private Integer absentSubscriberDiagnosticSM;
    private Integer additionalAbsentSubscriberDiagnosticSM;

}
