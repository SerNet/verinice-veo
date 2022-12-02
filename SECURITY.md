# Security Policy

## Searching for vulnerabilities

We explicitly allow ethical hackers to test our application, but only under these terms:

- Do not use off-the-shelf scanners, try to avoid useless traffic.
- Set a sensible rate limiting (e.g. 600/min)
- Do not access customer data, use your own accounts where possible.
- No phishing, no DDoS, no post-exploitation...

### Scope

To be clear, you're only allowed to test veo-web.verinice.com, api.verinice.com, auth.verinice.com, account.verinice.com and nothing else.
And of course you can use the open source code in our and other repositories:
- [github.com/SerNet/verinice-veo-web](https://github.com/SerNet/verinice-veo-web)
- [github.com/SerNet/verinice-veo](https://github.com/SerNet/verinice-veo)
- [github.com/SerNet/verinice-veo-accounts](https://github.com/SerNet/verinice-veo-accounts)
- [github.com/SerNet/verinice-veo-reporting](https://github.com/SerNet/verinice-veo-reporting)
- [github.com/SerNet/verincie-veo-history](https://github.com/SerNet/verincie-veo-history)
- [github.com/SerNet/verinice-veo-forms](https://github.com/SerNet/verinice-veo-forms)

## Reporting a vulnerability

Please e-mail security@verinice.com if you believe you have found a security issue in verinice.veo.

In your bug report, please try to cover the following info:

- Proof of Concept: exact steps to reproduce the bug
- How did you discover the vulnerability?
- Your estimation of impact
- Suggestions for a fix

When receiving a bug report, we will look at it internally before answering, so expect some delay until you get an answer.
Once we confirmed and talked about the vulnerability, we will contact you.

## Responsible Disclosure

The bug stays yours, but please give us enough time to fix it before disclosing it.
We may need up to 120 days to fix the vulnerability you reported. Once we've published a fix for the vulnerability and told you we're done from our end, you can disclose the bug.
