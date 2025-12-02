import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({

    resolve: {
        alias: {
            buffer: 'buffer', // 명시적 폴리필 매핑
        },
    },

    define: {
        global: 'window'
    },

    plugins: [react()],

})
