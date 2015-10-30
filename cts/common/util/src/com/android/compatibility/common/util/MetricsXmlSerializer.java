/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.compatibility.common.util;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.List;

/**
 * Serialize Metric data from {@link ReportLog} into compatibility report friendly XML
 */
public final class MetricsXmlSerializer {

    private final XmlSerializer mXmlSerializer;

    public MetricsXmlSerializer(XmlSerializer xmlSerializer) {
        this.mXmlSerializer = xmlSerializer;
    }

    public void serialize(ReportLog reportLog) throws IOException {
        if (reportLog == null) {
            return;
        }
        ReportLog.Result summary = reportLog.getSummary();
        List<ReportLog.Result> detailedMetrics = reportLog.getDetailedMetrics();
        // <Summary message="Average" scoreType="lower_better" unit="ms">195.2</Summary>
        if (summary != null) {
            mXmlSerializer.startTag(null, "Summary");
            mXmlSerializer.attribute(null, "message", summary.getMessage());
            mXmlSerializer.attribute(null, "scoreType", summary.getType().getXmlString());
            mXmlSerializer.attribute(null, "unit", summary.getUnit().getXmlString());
            mXmlSerializer.text(Double.toString(summary.getValues()[0]));
            mXmlSerializer.endTag(null, "Summary");
        }

        if (!detailedMetrics.isEmpty()) {
            mXmlSerializer.startTag(null, "Details");
            for (ReportLog.Result result : detailedMetrics) {
                mXmlSerializer.startTag(null, "ValueArray");
                mXmlSerializer.attribute(null, "source", result.getLocation());
                mXmlSerializer.attribute(null, "message", result.getMessage());
                mXmlSerializer.attribute(null, "scoreType", result.getType().getXmlString());
                mXmlSerializer.attribute(null, "unit", result.getUnit().getXmlString());

                for (double value : result.getValues()) {
                    mXmlSerializer.startTag(null, "Value");
                    mXmlSerializer.text(Double.toString(value));
                    mXmlSerializer.endTag(null, "Value");
                }
                mXmlSerializer.endTag(null, "ValueArray");
            }
            mXmlSerializer.endTag(null, "Details");
        }
    }
}
