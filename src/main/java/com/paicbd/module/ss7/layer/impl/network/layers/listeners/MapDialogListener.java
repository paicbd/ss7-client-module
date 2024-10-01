package com.paicbd.module.ss7.layer.impl.network.layers.listeners;

import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.Ss7Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPDialogListener;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.restcomm.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.restcomm.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName;

@Slf4j
@RequiredArgsConstructor
public class MapDialogListener implements MAPDialogListener {

    private final IChannelHandler channelHandler;

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogDelimiter");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog, AddressString addressString,
                                AddressString addressString1, MAPExtensionContainer mapExtensionContainer) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogRequest");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("addressString", addressString);
        channelMessage.setParameter("addressString1", addressString1);
        channelMessage.setParameter(Constants.MAP_EXTENSION_CONTAINER, mapExtensionContainer);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString addressString,
                                        AddressString addressString1, AddressString addressString2, AddressString addressString3) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogRequestEricsson");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("addressString", addressString);
        channelMessage.setParameter("addressString1", addressString1);
        channelMessage.setParameter("addressString3", addressString3);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer mapExtensionContainer) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogAccept");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter(Constants.MAP_EXTENSION_CONTAINER, mapExtensionContainer);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason mapRefuseReason,
                               ApplicationContextName applicationContextName, MAPExtensionContainer mapExtensionContainer) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogReject");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("mapRefuseReason", mapRefuseReason);
        channelMessage.setParameter(Constants.MAP_EXTENSION_CONTAINER, mapExtensionContainer);
        channelMessage.setParameter("applicationContextName", applicationContextName);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice mapUserAbortChoice,
                                  MAPExtensionContainer mapExtensionContainer) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogUserAbort");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("mapUserAbortChoice", mapUserAbortChoice);
        channelMessage.setParameter(Constants.MAP_EXTENSION_CONTAINER, mapExtensionContainer);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog,
                                      MAPAbortProviderReason mapAbortProviderReason, MAPAbortSource mapAbortSource,
                                      MAPExtensionContainer mapExtensionContainer) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogProviderAbort");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("mapAbortSource", mapAbortSource);
        channelMessage.setParameter(Constants.MAP_EXTENSION_CONTAINER, mapExtensionContainer);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        ChannelMessage channelMessage = Ss7Utils.getMessage(Constants.ON_DIALOG_CLOSE);
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog,
                               MAPNoticeProblemDiagnostic mapNoticeProblemDiagnostic) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogNotice");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        channelMessage.setParameter("mapNoticeProblemDiagnostic", mapNoticeProblemDiagnostic);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        ChannelMessage channelMessage = Ss7Utils.getMessage("onDialogRelease");
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        ChannelMessage channelMessage = Ss7Utils.getMessage(Constants.ON_DIALOG_TIMEOUT);
        channelMessage.setParameter(Constants.DIALOG, mapDialog);
        this.channelHandler.receiveMessageFromListener(channelMessage);
    }
}
