import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL =
  process.env.API_URL ||
  process.env.BACKEND_URL ||
  'http://localhost:8080';

function buildBackendUrl(path: string[], request: NextRequest) {
  const backendPath = path[0] === 'api' ? path.join('/') : ['api', ...path].join('/');
  const target = new URL(`/${backendPath}`, BACKEND_URL);
  request.nextUrl.searchParams.forEach((value, key) => {
    target.searchParams.append(key, value);
  });
  return target;
}

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const url = buildBackendUrl(path, request);
  const body =
    request.method === 'GET' || request.method === 'HEAD'
      ? undefined
      : await request.text();

  const tokenParam = request.nextUrl.searchParams.get('token');
  const cookieToken = request.cookies.get('legit_token')?.value || request.cookies.get('token')?.value;
  const authHeader =
    request.headers.get('authorization') ||
    (tokenParam ? `Bearer ${tokenParam}` : undefined) ||
    (cookieToken ? `Bearer ${cookieToken}` : undefined);

  const upstreamHeaders: Record<string, string> = {
    Accept: request.headers.get('accept') || 'application/json',
    'Content-Type': request.headers.get('content-type') || 'application/json',
  };

  if (authHeader) {
    upstreamHeaders['Authorization'] = authHeader;
  }

  const upstream = await fetch(url, {
    method: request.method,
    headers: upstreamHeaders,
    body,
    cache: 'no-store',
  });

  const responseBody = await upstream.text();

  const responseHeaders: Record<string, string> = {
    'Content-Type': upstream.headers.get('content-type') || 'application/json',
  };

  const contentDisposition = upstream.headers.get('content-disposition');
  if (contentDisposition) {
    responseHeaders['Content-Disposition'] = contentDisposition;
  }

  return new NextResponse(responseBody, {
    status: upstream.status,
    headers: responseHeaders,
  });
}

export async function GET(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}

export async function POST(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}

export async function PUT(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}
