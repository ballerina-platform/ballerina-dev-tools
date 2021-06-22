/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import React from "react";
import Highlight, { defaultProps } from "prism-react-renderer";
import { Popup } from 'semantic-ui-react'
import { rootPath } from "../Router"

import Prism from 'prism-react-renderer/prism';
(typeof global !== 'undefined' ? global : window).Prism = Prism;

require("../../public/prism-ballerina");

const timeoutLength = 1500
class CodeBlock extends React.Component {

    constructor(props) {
        super(props);
        this.state = { isOpen: false };
        this.handleOpen = this.handleOpen.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    handleOpen() {
        this.setState({ isOpen: true })

        this.timeout = setTimeout(() => {
            this.setState({ isOpen: false })
        }, timeoutLength)
    }

    handleClose() {
        this.setState({ isOpen: false })
        clearTimeout(this.timeout)
    }

    render() {
        const { inline, className, children } = this.props;
        if (children == null) {
            return (<></>);
        }
        // Extract langauge from className
        const match = /language-(\w+)/.exec(className || '')
        const language = match != null ? match[1] : "markdown";
        // Remove the \n at the end
        const code = String(children).replace(/\n$/, "");

        return !inline ? (
            <>
                <div className="copy-icon">
                    <Popup
                        trigger={<input title="Copy Code" type="image" src={rootPath + "content/copy-icon.svg"}
                            onClick={() => { navigator.clipboard.writeText(code) }} />}
                        content={<span>Copied!</span>}
                        on='click'
                        open={this.state.isOpen}
                        onClose={this.handleClose}
                        onOpen={this.handleOpen}
                        position='bottom center'
                    />
                </div>
                <Highlight {...defaultProps} code={code} language={language} theme={undefined} >
                    {({ className, style, tokens, getLineProps, getTokenProps }) => (
                        <pre className={className} style={style}>
                            {tokens.map((line, i) => (
                                <div {...getLineProps({ line, key: i })}>
                                    <span className='line-number'>{i + 1}</span>
                                    {line.map((token, key) => (
                                        <span  {...getTokenProps({ token, key })} />
                                    ))}
                                </div>
                            ))}
                        </pre>
                    )}
                </Highlight>
            </>
        ) : (
            <code className={className} {...this.props} />
        )

    }
}

export default CodeBlock;
