import {notFound} from 'next/navigation';
import Link from 'next/link';
import {helpArticles} from '../../../lib/content';
export const dynamicParams=false;
export function generateStaticParams(){return helpArticles.map(article=>({slug:article.slug}));}
export async function generateMetadata({params}:{params:Promise<{slug:string}>}){const slug=(await params).slug;return {title:`${helpArticles.find(article=>article.slug===slug)?.title||'Инструкция не найдена'} — Space Client`};}
export default async function Article({params}:{params:Promise<{slug:string}>}){const slug=(await params).slug;const item=helpArticles.find(article=>article.slug===slug);if(!item)notFound();return <article className="container article-body page-content"><div className="page-intro"><Link className="back-link" href="/help">Помощь /</Link><p className="eyebrow">ОБНОВЛЕНО 23.10.2026 · СТАДИЯ РАЗРАБОТКИ</p><h1>{item.title}</h1><p className="lead">{item.summary}</p></div><ol>{item.steps.map(step=><li key={step}>{step}</li>)}</ol><a className="text-link" href="https://github.com/AdilSatuv/spaceclient/issues">Открыть задачи и сообщения об ошибках ↗</a></article>;}
