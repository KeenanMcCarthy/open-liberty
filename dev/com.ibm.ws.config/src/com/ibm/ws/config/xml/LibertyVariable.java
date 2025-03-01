/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml;

public interface LibertyVariable {

    public enum Source {
        XML_CONFIG, BOOTSTRAP, FILE_SYSTEM, COMMAND_LINE
    };

    String getName();

    String getValue();

    boolean isSensitive();

    Source getSource();

    String getDefaultValue();

    String getObscuredValue();

    String getObscuredDefaultValue();
}
