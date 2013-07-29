/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */


package com.codenvy.analytics.client.presenter;

import com.codenvy.analytics.client.TimeLineServiceAsync;
import com.codenvy.analytics.metrics.TimeUnit;
import com.codenvy.analytics.shared.TableData;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;

import java.util.List;

/**
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public class TimeLineViewPresenter extends MainViewPresenter implements Presenter {
    private final TimeLineServiceAsync timelineService;

    public interface Display extends MainViewPresenter.Display {
        ListBox getTimeUnitBox();

        Button getSearchButton();

        String getUserEmail();
    }

    private TimeUnit currentTimeUnit = TimeUnit.DAY;

    public TimeLineViewPresenter(TimeLineServiceAsync timelineService, HandlerManager eventBus, Display view) {
        super(eventBus, view);
        this.timelineService = timelineService;

        update(currentTimeUnit, "");
    }

    public void bind() {
        getDisplay().getTimeUnitBox().addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                TimeUnit newCurrentTimeUnit = TimeUnit.values()[getDisplay().getTimeUnitBox().getSelectedIndex()];
                if (newCurrentTimeUnit != currentTimeUnit) {
                    currentTimeUnit = newCurrentTimeUnit;
                    update(currentTimeUnit, getDisplay().getUserEmail());
                }
            }
        });

        getDisplay().getSearchButton().addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                currentTimeUnit = TimeUnit.values()[getDisplay().getTimeUnitBox().getSelectedIndex()];
                update(currentTimeUnit, getDisplay().getUserEmail());
            }
        });

        super.bind();
    }

    private void update(TimeUnit timeUnit, String userFilter) {
        getDisplay().getGWTLoader().show();

        timelineService.getData(timeUnit, userFilter, new AsyncCallback<List<TableData>>() {
            public void onFailure(Throwable caught) {
                getDisplay().getGWTLoader().hide();
                getDisplay().getContentTable().setText(0, 0, caught.getMessage());
            }

            public void onSuccess(List<TableData> result) {
                getDisplay().getGWTLoader().hide();
                getDisplay().setData(result);
            }
        });
    }

    private Display getDisplay() {
        return (Display)display;
    }
}
