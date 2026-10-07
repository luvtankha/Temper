import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import { App } from './app/App';
import { publicDemo } from './app/publicMode';
import './styles/index.css';

const Router = publicDemo ? HashRouter : BrowserRouter;
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><Router><App /></Router></React.StrictMode>);
