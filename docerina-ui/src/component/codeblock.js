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


    state = { isOpen: false }

    handleOpen = () => {
      this.setState({ isOpen: true })
  
      this.timeout = setTimeout(() => {
        this.setState({ isOpen: false })
      }, timeoutLength)
    }
  
    handleClose = () => {
      this.setState({ isOpen: false })
      clearTimeout(this.timeout)
    }

    render() {
        const { language, value } = this.props;
        if (value == null) {
            return (<></>);
        }
        const cleanedVal = value.replace(/^(\s*#){1}/, "").replace(/\n(\s*#){1}/g, "\n").replace(/\n.?```.*\n?.*/, "");

        async function clipboardCopy() {
            await navigator.clipboard.writeText(cleanedVal);
        }

        return (
            <>
                <div className="copy-icon">
                    <Popup
                        trigger={<input title="Copy Code" type="image" src={rootPath + "content/copy-icon.svg"} onClick={clipboardCopy}/>}
                        content={<span>Copied!</span>}
                        on='click'
                        open={this.state.isOpen}
                        onClose={this.handleClose}
                        onOpen={this.handleOpen}
                        position='bottom center'
                    />
                </div>
                <Highlight {...defaultProps} code={cleanedVal} language={language} theme={undefined} >
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
        );
    }
}

export default CodeBlock;
