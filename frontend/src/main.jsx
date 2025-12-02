import {createRoot} from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import {Buffer} from 'buffer';
import process from 'process';

if (!window.Buffer) {
    // @ts-ignore
    window.Buffer = Buffer;
}

if (!window.process) {
    // @ts-ignore
    window.process = process;
}

createRoot(document.getElementById('root')).render(
    <App />,
)
