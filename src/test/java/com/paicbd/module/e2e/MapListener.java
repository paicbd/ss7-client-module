package com.paicbd.module.e2e;

import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPDialogListener;
import org.restcomm.protocols.ss7.map.api.MAPMessage;
import org.restcomm.protocols.ss7.map.api.MAPProvider;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.restcomm.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.restcomm.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.restcomm.protocols.ss7.map.api.dialog.ServingCheckData;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.NoteSubscriberPresentRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ReadyForSMRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ReadyForSMResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName;
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem;

@Slf4j
public abstract class MapListener implements MAPDialogListener, MAPServiceSmsListener, MAPServiceSms {
    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
        log.info("onDialogDelimiter for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog, AddressString addressString, AddressString addressString1, MAPExtensionContainer mapExtensionContainer) {
        log.info("onDialogRequest for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString addressString, AddressString addressString1, AddressString addressString2, AddressString addressString3) {
        log.info("onDialogRequestEricsson for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer mapExtensionContainer) {
        log.info("onDialogAccept for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason mapRefuseReason, ApplicationContextName applicationContextName, MAPExtensionContainer mapExtensionContainer) {
        log.info("onDialogReject for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice mapUserAbortChoice, MAPExtensionContainer mapExtensionContainer) {
        log.info("onDialogUserAbort for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason mapAbortProviderReason, MAPAbortSource mapAbortSource, MAPExtensionContainer mapExtensionContainer) {
        log.info("onDialogProviderAbort for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        log.info("onDialogClose for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic mapNoticeProblemDiagnostic) {
        log.info("onDialogNotice for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        log.info("onDialogRelease for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        log.info("onDialogTimeout for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public MAPDialogSms createNewDialog(MAPApplicationContext mapApplicationContext, SccpAddress sccpAddress, AddressString addressString, SccpAddress sccpAddress1, AddressString addressString1, Long aLong) {
        log.info("createNewDialog for MAPApplicationContext: {}", mapApplicationContext);
        return null;
    }

    @Override
    public ServingCheckData isServingService(MAPApplicationContext mapApplicationContext) {
        log.info("isServingService for MAPApplicationContext: {}", mapApplicationContext);
        return null;
    }

    @Override
    public boolean isActivated() {
        return false;
    }

    @Override
    public void activate() {
        log.info("activate");
    }

    @Override
    public void deactivate() {
        log.info("deactivate");
    }

    @Override
    public MAPProvider getMAPProvider() {
        log.info("getMAPProvider");
        return null;
    }

    @Override
    public MAPDialogSms createNewDialog(MAPApplicationContext mapApplicationContext, SccpAddress sccpAddress, AddressString addressString, SccpAddress sccpAddress1, AddressString addressString1) {
        log.info("MAPDialogSms createNewDialog for MAPApplicationContext: {}", mapApplicationContext);
        return null;
    }

    @Override
    public void addMAPServiceListener(MAPServiceSmsListener mapServiceSmsListener) {
        log.info("addMAPServiceListener");
    }

    @Override
    public void removeMAPServiceListener(MAPServiceSmsListener mapServiceSmsListener) {
        log.info("removeMAPServiceListener");
    }

    @Override
    public void onForwardShortMessageRequest(ForwardShortMessageRequest forwardShortMessageRequest) {
        log.info("onForwardShortMessageRequest for DialogId: {}", forwardShortMessageRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse forwardShortMessageResponse) {
        log.info("onForwardShortMessageResponse for DialogId: {}", forwardShortMessageResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwardShortMessageRequest) {
        log.info("onMoForwardShortMessageRequest for DialogId: {}", moForwardShortMessageRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwardShortMessageResponse) {
        log.info("onMoForwardShortMessageResponse for DialogId: {}", moForwardShortMessageResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwardShortMessageRequest) {
        log.info("onMtForwardShortMessageRequest for DialogId: {}", mtForwardShortMessageRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwardShortMessageResponse) {
        log.info("onMtForwardShortMessageResponse for DialogId: {}", mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMRequest) {
        log.info("onSendRoutingInfoForSMRequest for DialogId: {}", sendRoutingInfoForSMRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse) {
        log.info("onSendRoutingInfoForSMResponse for DialogId: {}", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusRequest) {
        log.info("onReportSMDeliveryStatusRequest for DialogId: {}", reportSMDeliveryStatusRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse reportSMDeliveryStatusResponse) {
        log.info("onReportSMDeliveryStatusResponse for DialogId: {}", reportSMDeliveryStatusResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreRequest) {
        log.info("onInformServiceCentreRequest for DialogId: {}", informServiceCentreRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreRequest) {
        log.info("onAlertServiceCentreRequest for DialogId: {}", alertServiceCentreRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreResponse) {
        log.info("onAlertServiceCentreResponse for DialogId: {}", alertServiceCentreResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onReadyForSMRequest(ReadyForSMRequest readyForSMRequest) {
        log.info("onReadyForSMRequest for DialogId: {}", readyForSMRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onReadyForSMResponse(ReadyForSMResponse readyForSMResponse) {
        log.info("onReadyForSMResponse for DialogId: {}", readyForSMResponse.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onNoteSubscriberPresentRequest(NoteSubscriberPresentRequest noteSubscriberPresentRequest) {
        log.info("onNoteSubscriberPresentRequest for DialogId: {}", noteSubscriberPresentRequest.getMAPDialog().getLocalDialogId());
    }

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long aLong, MAPErrorMessage mapErrorMessage) {
        log.info("onErrorComponent for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long aLong, Problem problem, boolean b) {
        log.info("onRejectComponent for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long aLong) {
        log.info("onInvokeTimeout for DialogId: {}", mapDialog.getLocalDialogId());
    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {
        log.info("onMAPMessage for DialogId: {}", mapMessage.getMAPDialog().getLocalDialogId());
    }
}
