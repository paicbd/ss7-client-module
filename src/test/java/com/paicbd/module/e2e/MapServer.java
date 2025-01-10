package com.paicbd.module.e2e;

import javolution.util.FastList;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.indicator.RoutingIndicator;
import org.restcomm.protocols.ss7.m3ua.ExchangeType;
import org.restcomm.protocols.ss7.m3ua.Functionality;
import org.restcomm.protocols.ss7.m3ua.IPSPType;
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.restcomm.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl;
import org.restcomm.protocols.ss7.m3ua.parameter.NetworkAppearance;
import org.restcomm.protocols.ss7.m3ua.parameter.RoutingContext;
import org.restcomm.protocols.ss7.m3ua.parameter.TrafficModeType;
import org.restcomm.protocols.ss7.map.MAPStackImpl;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.MAPProvider;
import org.restcomm.protocols.ss7.map.api.errors.AbsentSubscriberDiagnosticSM;
import org.restcomm.protocols.ss7.map.api.errors.CallBarringCause;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.primitives.LMSI;
import org.restcomm.protocols.ss7.map.api.primitives.NetworkResource;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MWStatus;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.AddressField;
import org.restcomm.protocols.ss7.map.api.smstpdu.CommandTypeValue;
import org.restcomm.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.restcomm.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserData;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.restcomm.protocols.ss7.map.primitives.AddressStringImpl;
import org.restcomm.protocols.ss7.map.primitives.IMSIImpl;
import org.restcomm.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.restcomm.protocols.ss7.map.primitives.LMSIImpl;
import org.restcomm.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_DAImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_OAImpl;
import org.restcomm.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.restcomm.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.restcomm.protocols.ss7.map.smstpdu.CommandDataImpl;
import org.restcomm.protocols.ss7.map.smstpdu.CommandTypeImpl;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.restcomm.protocols.ss7.map.smstpdu.FailureCauseImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsCommandTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsDeliverReportTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsSubmitTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ValidityPeriodImpl;
import org.restcomm.protocols.ss7.sccp.LoadSharingAlgorithm;
import org.restcomm.protocols.ss7.sccp.OriginationType;
import org.restcomm.protocols.ss7.sccp.Router;
import org.restcomm.protocols.ss7.sccp.RuleType;
import org.restcomm.protocols.ss7.sccp.SccpResource;
import org.restcomm.protocols.ss7.sccp.impl.SccpStackImpl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.restcomm.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.restcomm.protocols.ss7.sccp.parameter.EncodingScheme;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import org.restcomm.protocols.ss7.sccpext.impl.SccpExtModuleImpl;
import org.restcomm.protocols.ss7.sccpext.router.RouterExt;
import org.restcomm.protocols.ss7.ss7ext.Ss7ExtInterface;
import org.restcomm.protocols.ss7.ss7ext.Ss7ExtInterfaceImpl;
import org.restcomm.protocols.ss7.tcap.TCAPStackImpl;
import org.restcomm.protocols.ss7.tcap.asn.ReturnResultLastImpl;
import org.restcomm.protocols.ss7.tcap.asn.comp.ReturnResultLast;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.restcomm.protocols.ss7.sccp.LongMessageRuleType.XUDT_ENABLED;

@Slf4j
@RequiredArgsConstructor
public class MapServer extends MapListener {
    // MTP Details
    private static final int ORIGINATING_PC = 200;
    private static final int DESTINATION_PC = 100;
    private static final int NETWORK_INDICATOR = 2;
    private static final int SERVICE_INDICATOR = 3; // SCCP
    private static final int SMSC_SSN = 8;
    private static final int HLR_SSN = 6;
    private static final int MSC_SSN = 8;

    // M3UA details
    private static final String HOST_IP = "127.0.0.1";
    private static final String PEER_IP = "127.0.0.1";

    private static final Functionality AS_FUNCTIONALITY = Functionality.valueOf("IPSP");
    private static final int ROUTING_CONTEXT = 101;
    private static final int NETWORK_APPEARANCE = 102;
    private static final int DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = 1;
    private static final String SERVER_ASSOCIATION_NAME = "serverAssociation";
    private static final String SERVER_NAME = "mockServer";


    // TCAP Details
    private static final int MAX_DIALOGS = 500000;
    private static final ParameterFactoryImpl factory = new ParameterFactoryImpl();
    private static final RoutingIndicator ROUTING_INDICATOR = RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN;

    private final String path;
    private final int hostPort;
    private final int peerPort;

    private TCAPStackImpl tcapStack;
    private SccpStackImpl sccpStack;
    private M3UAManagementImpl serverM3UAMgmt;
    private NettySctpManagementImpl sctpManagement;
    private MAPProvider mapProvider;

    @Setter
    private SendRoutingInfoForSmReaction sendRoutingInfoForSmReaction = SendRoutingInfoForSmReaction.RETURN_SUCCESS;

    @Setter
    private MtForwardSmReaction mtForwardSMReaction = MtForwardSmReaction.RETURN_SUCCESS;

    @Setter
    private InformServiceCentreReaction informServiceCentreReaction = InformServiceCentreReaction.MWD_NO;

    public void initializeStack(IpChannelType ipChannelType) throws Exception {
        this.initSCTP(ipChannelType);
        this.initM3UA();
        this.initSCCP();
        this.initTCAP();
        this.initMAP();
        serverM3UAMgmt.startAsp("ASP1");
    }

    public void stopStack() throws Exception {
        this.serverM3UAMgmt.removeAllResources();
        this.serverM3UAMgmt.stop();
        this.sctpManagement.stop();
        this.sccpStack.stop();
        this.tcapStack.stop();
    }

    private void initSCTP(IpChannelType ipChannelType) throws Exception {
        this.sctpManagement = new NettySctpManagementImpl("Server");
        this.sctpManagement.setPersistDir(this.path);
        this.sctpManagement.start();
        this.sctpManagement.setConnectDelay(10000);
        this.sctpManagement.removeAllResources();

        this.sctpManagement.addServer(SERVER_NAME, HOST_IP, this.hostPort, ipChannelType, null);
        this.sctpManagement.addServerAssociation(PEER_IP, this.peerPort, SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);
        this.sctpManagement.startServer(SERVER_NAME);
    }

    private void initM3UA() throws Exception {
        this.serverM3UAMgmt = new M3UAManagementImpl("Server", null, new Ss7ExtInterfaceImpl());
        this.serverM3UAMgmt.setTransportManagement(this.sctpManagement);
        this.serverM3UAMgmt.setDeliveryMessageThreadCount(DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);
        this.serverM3UAMgmt.setPersistDir(this.path);
        this.serverM3UAMgmt.start();
        this.serverM3UAMgmt.removeAllResources();

        RoutingContext rc = factory.createRoutingContext(new long[]{ROUTING_CONTEXT});
        TrafficModeType trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare);
        NetworkAppearance na = factory.createNetworkAppearance(NETWORK_APPEARANCE);

        IPSPType ipspType = null;
        if (Objects.equals(AS_FUNCTIONALITY, Functionality.IPSP)) {
            ipspType = IPSPType.SERVER;
        }

        this.serverM3UAMgmt.createAs("AS1", AS_FUNCTIONALITY, ExchangeType.SE, ipspType, rc, trafficModeType, 1, na);
        this.serverM3UAMgmt.createAspFactory("ASP1", SERVER_ASSOCIATION_NAME);
        this.serverM3UAMgmt.assignAspToAs("AS1", "ASP1");
        this.serverM3UAMgmt.addRoute(DESTINATION_PC, ORIGINATING_PC, SERVICE_INDICATOR, "AS1");
    }

    private void initSCCP() throws Exception {
        Ss7ExtInterface ss7ExtInterface = new Ss7ExtInterfaceImpl();
        SccpExtModuleImpl sccpExtModule = new SccpExtModuleImpl();
        ss7ExtInterface.setSs7ExtSccpInterface(sccpExtModule);
        this.sccpStack = new SccpStackImpl("MapLoadServerSccpStack", ss7ExtInterface);
        this.sccpStack.setMtp3UserPart(1, this.serverM3UAMgmt);
        this.sccpStack.setPersistDir(this.path);
        this.sccpStack.start();
        this.sccpStack.removeAllResources();

        Router router = this.sccpStack.getRouter();
        RouterExt routerExt = sccpExtModule.getRouterExt();
        SccpResource sccpResource = this.sccpStack.getSccpResource();

        sccpResource.addRemoteSpc(0, DESTINATION_PC, 0, 0);
        sccpResource.addRemoteSsn(0, DESTINATION_PC, SMSC_SSN, 0, false);
        sccpResource.addRemoteSsn(1, DESTINATION_PC, HLR_SSN, 0, false);

        router.addMtp3ServiceAccessPoint(1, 1, ORIGINATING_PC, NETWORK_INDICATOR, 0, null);
        router.addMtp3Destination(1, 1, DESTINATION_PC, DESTINATION_PC, 0, 255, 255);
        router.addLongMessageRule(0, 1, 16384, XUDT_ENABLED);

        org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl fact = new org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl();
        EncodingScheme ec = new BCDEvenEncodingScheme();
        GlobalTitle gt1 = fact.createGlobalTitle("-", 0, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
                ec, NatureOfAddress.INTERNATIONAL);
        GlobalTitle gt2 = fact.createGlobalTitle("-", 0, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
                ec, NatureOfAddress.INTERNATIONAL);
        SccpAddress localAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt1, ORIGINATING_PC, 0);
        routerExt.addRoutingAddress(1, localAddress);
        SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt2, DESTINATION_PC, 0);
        routerExt.addRoutingAddress(2, remoteAddress);

        GlobalTitle gt = fact.createGlobalTitle("*", 0, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        SccpAddress pattern = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0);
        routerExt.addRule(1, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.REMOTE, pattern,
                "K", 1, -1, null, 0, null);
        routerExt.addRule(2, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.LOCAL, pattern,
                "K", 2, -1, null, 0, null);
    }

    private void initTCAP() throws Exception {
        List<Integer> extraSsns = new FastList<>();
        extraSsns.add(HLR_SSN);
        this.tcapStack = new TCAPStackImpl("TestServer", this.sccpStack.getSccpProvider(), MSC_SSN);
        this.tcapStack.setExtraSsns(extraSsns);
        this.tcapStack.setPersistDir(this.path);
        this.tcapStack.start();
        this.tcapStack.setDialogIdleTimeout(60000);
        this.tcapStack.setInvokeTimeout(30000);
        this.tcapStack.setMaxDialogs(MAX_DIALOGS);
    }

    private void initMAP() throws Exception {
        MAPStackImpl mapStack = new MAPStackImpl("TestServer", this.tcapStack.getProvider());
        mapProvider = mapStack.getMAPProvider();
        mapStack.setPersistDir(this.path);
        mapProvider.addMAPDialogListener(this);
        mapProvider.getMAPServiceSms().addMAPServiceListener(this);

        mapProvider.getMAPServiceSms().activate();
        mapStack.start();
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMRequestIndication) {
        log.debug("onSendRoutingInfoForSMRequest for DialogId={}, MapProvider={}, sendRoutingInfoForSmReaction={}", sendRoutingInfoForSMRequestIndication
                .getMAPDialog().getLocalDialogId(), mapProvider, sendRoutingInfoForSmReaction);

        try {
            long invokeId = sendRoutingInfoForSMRequestIndication.getInvokeId();
            MAPDialogSms mapDialogSms = sendRoutingInfoForSMRequestIndication.getMAPDialog();
            switch (sendRoutingInfoForSmReaction) {
                case RETURN_SUCCESS -> sriHandlerBySuccessRate(mapDialogSms, invokeId);
                case ERROR_ABSENT_SUBSCRIBER -> mapHandlerByAbsentSubscriber(mapDialogSms, invokeId);
                case ERROR_CALL_BARRED -> sriHandlerByCallBarred(mapDialogSms, invokeId);
                default -> throw new IllegalStateException("Unexpected value: " + sendRoutingInfoForSmReaction);
            }

            if (this.informServiceCentreReaction != InformServiceCentreReaction.MWD_NO) {
                MWStatus mwStatus = switch (this.informServiceCentreReaction) {
                    case InformServiceCentreReaction.MWD_MNRF ->
                            mapProvider.getMAPParameterFactory().createMWStatus(false, true, false, false);
                    case InformServiceCentreReaction.MWD_MCEF ->
                            mapProvider.getMAPParameterFactory().createMWStatus(false, false, true, false);
                    default -> null;
                };
                if (mwStatus != null) {
                    mapDialogSms.addInformServiceCentreRequest(null, mwStatus, null, null, null);
                }
            }
            mapDialogSms.close(false);
        } catch (MAPException e) {
            log.error("Error while sending SendRoutingInfoForSMRequest ", e);
        }
    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwardShortMessageRequestIndication) {
        log.debug("onMtForwardShortMessageRequest for DialogId={}, MapProvider={}", mtForwardShortMessageRequestIndication
                .getMAPDialog().getLocalDialogId(), mapProvider);
        try {
            long invokeId = mtForwardShortMessageRequestIndication.getInvokeId();
            MAPDialogSms mapDialogSms = mtForwardShortMessageRequestIndication.getMAPDialog();
            mapDialogSms.setUserObject(invokeId);
            switch (mtForwardSMReaction) {
                case RETURN_SUCCESS -> mtHandlerBySuccessRate(mapDialogSms, invokeId);
                case ERROR_ABSENT_SUBSCRIBER -> mapHandlerByAbsentSubscriber(mapDialogSms, invokeId);
                case ERROR_SYSTEM_FAILURE -> mapHandlerBySystemFailure(mapDialogSms, invokeId);
                case ERROR_SYSTEM_FAILURE_MEMORY_CAPACITY_EXCEEDED ->
                        mapHandlerBySystemFailureMemoryCapacityExceeded(mapDialogSms, invokeId);
                case ERROR_SYSTEM_FAILURE_UNKNOWN_SERVICE_CENTRE -> mapHandlerByBySystemFailureUnknownServiceCentre(mapDialogSms, invokeId);
            }
            mapDialogSms.close(false);
        } catch (MAPException e) {
            log.error("Error while sending MtForwardShortMessageRequest result ", e);
        }
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwardShortMessageRequestIndication) {
        log.debug("onMoForwardShortMessageRequest for DialogId={}, MapProvider={}", moForwardShortMessageRequestIndication
                .getMAPDialog().getLocalDialogId(), mapProvider);
        try {
            long invokeId = moForwardShortMessageRequestIndication.getInvokeId();
            MAPDialogSms mapDialogSms = moForwardShortMessageRequestIndication.getMAPDialog();
            mapDialogSms.setUserObject(invokeId);
            mapDialogSms.close(false);
        } catch (MAPException e) {
            log.error("Error while sending MoForwardShortMessageRequest ", e);
        }
    }

    @Override
    public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusRequestIndication) {
        MAPDialogSms curDialog = reportSMDeliveryStatusRequestIndication.getMAPDialog();
        long invokeId = reportSMDeliveryStatusRequestIndication.getInvokeId();
        try {
            curDialog.addReportSMDeliveryStatusResponse(invokeId, null, null);
            curDialog.close(false);
        } catch (MAPException e) {
            log.error("Error while handler ReportSMDeliveryStatusRequest", e);
        }
    }

    private LocationInfoWithLMSI getLocationInfoWithLMSI() {
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "598991900032");
        byte[] lmsiByte = null;
        Random rand = new Random();
        int lmsiRandom = rand.nextInt(4) + 1;
        lmsiByte = switch (lmsiRandom) {
            case 1 -> new byte[]{114, 2, (byte) 233, (byte) 140};
            case 2 -> new byte[]{113, (byte) 255, (byte) 172, (byte) 206};
            case 3 -> new byte[]{114, 2, (byte) 235, 55};
            case 4 -> new byte[]{114, 2, (byte) 231, (byte) 213};
            default -> lmsiByte;
        };
        LMSI lmsi = new LMSIImpl(lmsiByte);
        boolean gprsNodeIndicator = false;
        return new LocationInfoWithLMSIImpl(networkNodeNumber, lmsi, null,
                gprsNodeIndicator, null);
    }

    private void sriHandlerBySuccessRate(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        IMSI imsi = new IMSIImpl("748031234567890");
        LocationInfoWithLMSI locationInfoWithLMSI = getLocationInfoWithLMSI();
        mapDialogSms.addSendRoutingInfoForSMResponse(invokeId, imsi, locationInfoWithLMSI, null, null, null);
    }

    private void sriHandlerByCallBarred(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        MAPErrorMessage mapErrorMessage = mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageCallBarred(
                (long) mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion(), CallBarringCause.operatorBarring, null, null);
        mapDialogSms.sendErrorComponent(invokeId, mapErrorMessage);
    }

    private void mtHandlerBySuccessRate(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        log.debug("Sending MtForwardShortMessageRequest result with 100% success rate");
        ReturnResultLast returnResultLast = new ReturnResultLastImpl();
        returnResultLast.setInvokeId(invokeId);
        mapDialogSms.sendReturnResultLastComponent(returnResultLast);
    }

    private void mapHandlerByAbsentSubscriber(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        MAPErrorMessage mapErrorMessage = mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageAbsentSubscriberSM(
                AbsentSubscriberDiagnosticSM.IMSIDetached, null, null);
        mapDialogSms.sendErrorComponent(invokeId, mapErrorMessage);
    }

    private void mapHandlerBySystemFailure(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        MAPErrorMessage mapErrorMessage = mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
                mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion(),
                NetworkResource.vmsc, null, null);
        mapDialogSms.sendErrorComponent(invokeId, mapErrorMessage);
    }

    private void mapHandlerBySystemFailureMemoryCapacityExceeded(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        MAPErrorMessage mapErrorMessage = mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSMDeliveryFailure(
                mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion(),
                SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded, null, null);
        mapDialogSms.sendErrorComponent(invokeId, mapErrorMessage);
    }

    private void mapHandlerByBySystemFailureUnknownServiceCentre(MAPDialogSms mapDialogSms, long invokeId) throws MAPException {
        MAPErrorMessage mapErrorMessage = mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSMDeliveryFailure(
                mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion(),
                SMEnumeratedDeliveryFailureCause.unknownServiceCentre, null, null);
        mapDialogSms.sendErrorComponent(invokeId, mapErrorMessage);
    }

    public void sendMoForwardShortMessage(SmsTpduType type, DataCodingSchemeImpl dataCodingScheme) throws MAPException {
        AddressString origRef = mapProvider.getMAPParameterFactory()
                .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "50587771121");
        AddressString destRef = mapProvider.getMAPParameterFactory()
                .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "50570901121");

        SccpAddress clientSccpAddress = createSccpAddress(ORIGINATING_PC, "50588888888");
        SccpAddress serverSccpAddress = createSccpAddress(DESTINATION_PC, "50577777777");
        MAPDialogSms mapDialogSms = mapProvider.getMAPServiceSms().createNewDialog(MAPApplicationContext
                        .getInstance(MAPApplicationContextName.shortMsgMORelayContext, MAPApplicationContextVersion.version3),
                clientSccpAddress, origRef, serverSccpAddress, destRef);

        AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "50589898989");
        SM_RP_DA smRpDa = new SM_RP_DAImpl(serviceCentreAddressDA);
        ISDNAddressString msisdn = mapProvider.getMAPParameterFactory()
                .createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, "50584435612");
        SM_RP_OAImpl smRpOa = new SM_RP_OAImpl();
        smRpOa.setMsisdn(msisdn);

        boolean rejectDuplicates = true;
        boolean replyPathExists = false;
        boolean statusReportRequest = true;
        int messageReference = 187;
        AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber,
                NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "50578781231");
        ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(0);
        ValidityPeriod validityPeriod = new ValidityPeriodImpl(3);
        UserDataHeader userDataHeader = new UserDataHeaderImpl();
        Charset gsm8Charset = Charset.defaultCharset();
        UserData userData = new UserDataImpl("MO Message from Integration Test", dataCodingScheme, userDataHeader, gsm8Charset);

        SmsTpduImpl smsTpdu = switch (type) {
            case SmsTpduType.SMS_SUBMIT ->
                    new SmsSubmitTpduImpl(rejectDuplicates, replyPathExists, statusReportRequest, messageReference, destinationAddress,
                            protocolIdentifier, validityPeriod, userData);
            case SmsTpduType.SMS_COMMAND -> new SmsCommandTpduImpl(false, messageReference, protocolIdentifier,
                    new CommandTypeImpl(CommandTypeValue.Reserved), 0,
                    destinationAddress, new CommandDataImpl("0"));
            case SmsTpduType.SMS_DELIVER_REPORT ->
                    new SmsDeliverReportTpduImpl(new FailureCauseImpl(88), protocolIdentifier, userData);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        SmsSignalInfo smsSignalInfo = new SmsSignalInfoImpl(smsTpdu, gsm8Charset);
        IMSI imsi = new IMSIImpl("748031234567890");

        mapDialogSms.addMoForwardShortMessageRequest(smRpDa, smRpOa, smsSignalInfo, null, imsi);
        mapDialogSms.send();
    }

    public void sendAlertServiceCentre(String destinationAddr) {
        try {
            MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version2);
            ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(
                    AddressNature.international_number, NumberingPlan.ISDN, destinationAddr);

            AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number,
                    NumberingPlan.ISDN, "50589898989");

            SccpAddress clientSccpAddress = createSccpAddress(ORIGINATING_PC, "50588888888");
            SccpAddress serverSccpAddress = createSccpAddress(DESTINATION_PC, "50577777777");

            MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(mapAppContext, clientSccpAddress,
                    null, serverSccpAddress, null);

            curDialog.addAlertServiceCentreRequest(msisdn, serviceCentreAddressDA);
            curDialog.send();

        } catch (MAPException ex) {
            log.error("Error on sendAlertServiceCentre ", ex);
        }
    }

    private SccpAddress createSccpAddress(int dpc, String gtDigits) {
        org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl fact = new org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl();
        GlobalTitle gt = fact.createGlobalTitle(gtDigits, 0, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
                BCDEvenEncodingScheme.INSTANCE, NatureOfAddress.INTERNATIONAL);
        return fact.createSccpAddress(ROUTING_INDICATOR, gt, dpc, 8);
    }
}
