'use client';
export default function ErrorPage({reset}:{reset:()=>void}){return <section className="container page-content"><div className="page-intro"><p className="eyebrow">СБОЙ СВЯЗИ</p><h1>Не удалось открыть страницу.</h1><p className="lead">Попробуйте ещё раз или вернитесь позже.</p><button className="button" onClick={reset}>Повторить</button></div></section>;}
