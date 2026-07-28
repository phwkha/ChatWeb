import React from 'react'

function App() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white selection:bg-indigo-500 selection:text-white">
      <div className="text-center space-y-6 max-w-lg p-8 bg-slate-800/50 rounded-3xl border border-white/5 shadow-2xl backdrop-blur-xl">
        <div className="flex justify-center">
          <div className="w-20 h-20 bg-indigo-500 rounded-2xl rotate-12 flex items-center justify-center shadow-lg shadow-indigo-500/30">
            <span className="text-4xl -rotate-12">🚀</span>
          </div>
        </div>
        <h1 className="text-4xl font-bold bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">
          ChatWeb Frontend
        </h1>
        <p className="text-slate-400 text-lg">
          Project đã được dọn dẹp sạch sẽ và sẵn sàng xây dựng các tính năng mới với TailwindCSS v4!
        </p>
        <div className="flex gap-4 justify-center pt-4">
          <button className="px-6 py-3 bg-indigo-500 hover:bg-indigo-400 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-500/25 active:scale-95 cursor-pointer">
            Bắt đầu code
          </button>
        </div>
      </div>
    </div>
  )
}

export default App
