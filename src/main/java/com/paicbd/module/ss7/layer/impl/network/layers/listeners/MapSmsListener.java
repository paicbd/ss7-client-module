package com.paicbd.module.ss7.layer.impl.network.layers.listeners;

import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.utils.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPMessage;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
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
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem;


@Slf4j
@Generated
@RequiredArgsConstructor
public class MapSmsListener implements MAPServiceSmsListener {

    private final IChannelHandler channelHandler;

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId,
                                 MAPErrorMessage mapErrorMessage) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(Constants.ON_ERROR_COMPONENT);
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter(Constants.INVOKE_ID, invokeId);
        channelMessage.setParameter(Constants.MAP_ERROR_MESSAGE, mapErrorMessage);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem,
                                  boolean isLocalOriginated) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(Constants.ON_REJECT_COMPONENT);
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter(Constants.INVOKE_ID, invokeId);
        channelMessage.setParameter(Constants.PROBLEM, problem);
        channelMessage.setParameter(Constants.LOCAL_ORIGINATED, isLocalOriginated);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(Constants.ON_INVOKE_TIMEOUT);
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter(Constants.INVOKE_ID, invokeId);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(Constants.ON_MAP_MESSAGE);
        channelMessage.setParameter(Constants.MAP_MESSAGE, mapMessage);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onForwardShortMessageRequest(ForwardShortMessageRequest forwSmInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(forwSmInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, forwSmInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse forwSmRespInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(forwSmRespInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, forwSmRespInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwSmInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(moForwSmInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, moForwSmInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(moForwSmRespInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, moForwSmRespInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(mtForwSmInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, mtForwSmInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwSmRespInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(mtForwSmRespInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, mtForwSmRespInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(sendRoutingInfoForSMInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, sendRoutingInfoForSMInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMRespInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(sendRoutingInfoForSMRespInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, sendRoutingInfoForSMRespInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onReportSMDeliveryStatusRequest(
            ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(reportSMDeliveryStatusInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, reportSMDeliveryStatusInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onReportSMDeliveryStatusResponse(
            ReportSMDeliveryStatusResponse reportSMDeliveryStatusRespInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(reportSMDeliveryStatusRespInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, reportSMDeliveryStatusRespInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(informServiceCentreInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, informServiceCentreInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(alertServiceCentreInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, alertServiceCentreInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(alertServiceCentreInd.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, alertServiceCentreInd);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onReadyForSMRequest(ReadyForSMRequest request) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(request.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, request);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onReadyForSMResponse(ReadyForSMResponse response) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(response.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, response);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onNoteSubscriberPresentRequest(NoteSubscriberPresentRequest request) {
        ChannelMessage channelMessage = Ss7Utils.createChannelMessage(request.getMessageType().toString());
        channelMessage.setParameter(Constants.MESSAGE, request);
        channelHandler.receiveMessageFromListener(channelMessage);
    }

}
