using System.Net.Http.Json;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.IO;

namespace LightTodo.Windows;

public static class AccountRules {
    public static string? ValidateEmail(string? email)=>string.IsNullOrWhiteSpace(email)||!System.Net.Mail.MailAddress.TryCreate(email.Trim(),out _)?"请输入有效的邮箱地址":null;
    public static string? ValidatePassword(string? password)=>password?.Length<8?"密码至少需要 8 个字符":password==null||!password.Any(char.IsLetter)||!password.Any(char.IsDigit)?"密码需要同时包含字母和数字":null;
}
public sealed record AccountResult(bool Success,string Message,string Email="",string AccessToken="",string RefreshToken="") {
    public static AccountResult Error(string message)=>new(false,message);
}
public interface IAccountAuth { Task<AccountResult> SignIn(string email,string password);Task<AccountResult> SignUp(string email,string password);Task<AccountResult> ResetPassword(string email); }
public sealed class SupabaseAccountAuth : IAccountAuth {
    readonly HttpClient client=new();readonly string url=Environment.GetEnvironmentVariable("LIGHTTODO_SUPABASE_URL")?.TrimEnd('/')??"",key=Environment.GetEnvironmentVariable("LIGHTTODO_SUPABASE_ANON_KEY")??"";
    public Task<AccountResult> SignIn(string email,string password)=>Send("/auth/v1/token?grant_type=password",new{email,password},email,false);
    public Task<AccountResult> SignUp(string email,string password)=>Send("/auth/v1/signup",new{email,password},email,true);
    public async Task<AccountResult> ResetPassword(string email){var result=await Send("/auth/v1/recover",new{email},email,false);return result.Success?result with{Message="密码重置邮件已发送"}:result;}
    async Task<AccountResult> Send(string path,object body,string email,bool signup){if(url.Length==0||key.Length==0)return AccountResult.Error("尚未配置云端账户服务");try{using var request=new HttpRequestMessage(HttpMethod.Post,url+path){Content=JsonContent.Create(body)};request.Headers.Add("apikey",key);request.Headers.Authorization=new("Bearer",key);using var response=await client.SendAsync(request);var text=await response.Content.ReadAsStringAsync();using var json=JsonDocument.Parse(string.IsNullOrWhiteSpace(text)?"{}":text);var root=json.RootElement;if(response.IsSuccessStatusCode){var access=Value(root,"access_token");return access.Length>0?new(true,"登录成功",email,access,Value(root,"refresh_token")):new(true,signup?"验证邮件已发送，请查收后登录":"请求已完成",email);}var message=Value(root,"msg");if(message.Length==0)message=Value(root,"error_description");return AccountResult.Error(Friendly(message));}catch{return AccountResult.Error("无法连接账户服务，请检查网络后重试");}}
    static string Value(JsonElement root,string name)=>root.TryGetProperty(name,out var value)?value.GetString()??"":"";
    static string Friendly(string value){var lower=value.ToLowerInvariant();if(lower.Contains("invalid login"))return "邮箱或密码不正确";if(lower.Contains("already registered"))return "该邮箱已经注册";if(lower.Contains("rate limit"))return "请求过于频繁，请稍后再试";return value.Length==0?"账户请求失败":value;}
}
public sealed class WindowsAccountSession {
    readonly string path;public string Email{get;private set;}="";public string AccessToken{get;private set;}="";public string RefreshToken{get;private set;}="";public bool IsSignedIn=>AccessToken.Length>0;
    public WindowsAccountSession(string? path=null){this.path=path??Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"LightTodo","account.dat");Load();}
    public void Save(AccountResult result){if(!result.Success||result.AccessToken.Length==0)throw new ArgumentException("Signed-in result required");Email=result.Email;AccessToken=result.AccessToken;RefreshToken=result.RefreshToken;Directory.CreateDirectory(Path.GetDirectoryName(path)!);File.WriteAllBytes(path,Protect(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new[]{Email,AccessToken,RefreshToken}))));}
    public void Clear(){Email=AccessToken=RefreshToken="";if(File.Exists(path))File.Delete(path);}
    void Load(){try{if(!File.Exists(path))return;var values=JsonSerializer.Deserialize<string[]>(Encoding.UTF8.GetString(Unprotect(File.ReadAllBytes(path))));if(values?.Length==3){Email=values[0];AccessToken=values[1];RefreshToken=values[2];}}catch{Clear();}}
    [StructLayout(LayoutKind.Sequential)]struct Blob{public int Size;public IntPtr Data;}
    [DllImport("crypt32.dll",SetLastError=true)]static extern bool CryptProtectData(ref Blob input,string? description,IntPtr entropy,IntPtr reserved,IntPtr prompt,int flags,out Blob output);
    [DllImport("crypt32.dll",SetLastError=true)]static extern bool CryptUnprotectData(ref Blob input,IntPtr description,IntPtr entropy,IntPtr reserved,IntPtr prompt,int flags,out Blob output);
    [DllImport("kernel32.dll")]static extern IntPtr LocalFree(IntPtr memory);
    static byte[] Protect(byte[] data)=>Crypt(data,true);static byte[] Unprotect(byte[] data)=>Crypt(data,false);
    static byte[] Crypt(byte[] data,bool protect){var input=new Blob{Size=data.Length,Data=Marshal.AllocHGlobal(data.Length)};try{Marshal.Copy(data,0,input.Data,data.Length);Blob output;var ok=protect?CryptProtectData(ref input,null,IntPtr.Zero,IntPtr.Zero,IntPtr.Zero,0,out output):CryptUnprotectData(ref input,IntPtr.Zero,IntPtr.Zero,IntPtr.Zero,IntPtr.Zero,0,out output);if(!ok)throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());try{var result=new byte[output.Size];Marshal.Copy(output.Data,result,0,result.Length);return result;}finally{LocalFree(output.Data);}}finally{Marshal.FreeHGlobal(input.Data);}}
}
