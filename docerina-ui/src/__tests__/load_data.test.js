/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import React from 'react';
import { render, unmountComponentAtNode } from 'react-dom';
import { act } from 'react-dom/test-utils';
import '@testing-library/jest-dom';
import docJson from './api-docs.json';
import App from '../App';

window.scrollTo = jest.fn();

// before each test, create `div` element
beforeEach(() => {
    const elem = document.createElement('div'); // <div>...
    elem.setAttribute('id', 'app'); // <div id="app">...
    document.body.appendChild(elem); // <body><div>...
});

// after each test, remove `div` element
afterEach(() => {
    const elem = document.getElementById('app');
    unmountComponentAtNode(elem); // unmount React component
    elem.remove(); // remove
});

test('Test data loading for API Docs React App', () => {
    const elem = document.getElementById('app');
    act(() => {
        render(<App data={docJson}/>, elem);
    });

    const h2Elem = elem.querySelector('h2'); // <h1>
    expect(h2Elem).toHaveTextContent('Language Libraries');
});
